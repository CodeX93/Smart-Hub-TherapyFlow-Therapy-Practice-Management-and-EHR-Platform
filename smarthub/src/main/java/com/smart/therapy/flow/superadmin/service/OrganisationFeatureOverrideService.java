package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.PlanFeatureVersion;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import com.smart.therapy.flow.subscription.repository.PlanFeatureVersionRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrganisationFeatureOverrideService {

    private final OrganisationRepository organisationRepository;
    private final OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    private final PlanFeatureVersionRepository planFeatureVersionRepository;
    private final PlatformAuditService platformAuditService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final ObjectMapper objectMapper;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    @Value("${app.redis.fail-open:true}")
    private boolean redisFailOpen;

    @Transactional(readOnly = true)
    public Map<String, FeatureState> getEffectiveFeatures(Long organisationId) {
        Organisation org = requireOrganisation(organisationId);
        Instant now = Instant.now();

        LinkedHashMap<String, FeatureState> effective = new LinkedHashMap<>();
        for (CoreFeature feature : CoreFeature.values()) {
            effective.put(feature.getCode(), new FeatureState(feature.getCatalogDefaultEnabled(), feature.getCatalogDefaultUsageLimit()));
        }

        OrgSubscription sub = subscriptionFeatureService.getCurrentSubscription(org.getId());
        if (sub != null) {
            List<PlanFeatureVersion> planFeatures = planFeatureVersionRepository.findByPlanIdEffectiveAt(sub.getPlan().getId(), now);
            for (PlanFeatureVersion planFeature : planFeatures) {
                CoreFeature coreFeature = CoreFeature.fromCode(planFeature.getFeature().getCode());
                if (coreFeature == null) {
                    continue;
                }
                effective.put(coreFeature.getCode(),
                        new FeatureState(Boolean.TRUE.equals(planFeature.getIsEnabled()), planFeature.getUsageLimit()));
            }
        }

        List<OrgFeatureOverride> overrides = orgFeatureOverrideRepository.findByOrganisationId(org.getId());
        for (OrgFeatureOverride override : overrides) {
            CoreFeature coreFeature = CoreFeature.fromCode(override.getFeatureKey());
            if (coreFeature == null) {
                continue;
            }
            effective.put(coreFeature.getCode(),
                    new FeatureState(Boolean.TRUE.equals(override.getEnabled()), override.getUsageLimit()));
        }

        return effective;
    }

    @Transactional
    public Map<String, FeatureState> replaceOverrides(Long organisationId,
                                                      Map<String, FeatureState> requested,
                                                      Long actorAuthId) {
        Organisation org = requireOrganisation(organisationId);
        if (requested == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "features is required");
        }

        Map<String, FeatureState> normalized = normalizeAndValidate(requested);
        List<OrgFeatureOverride> before = orgFeatureOverrideRepository.findByOrganisationId(org.getId());
        Map<String, OrgFeatureOverride> existingByFeatureKey = new HashMap<>();
        List<OrgFeatureOverride> duplicateRows = new ArrayList<>();
        for (OrgFeatureOverride override : before) {
            String normalizedFeatureKey = normalizeFeatureKey(override.getFeatureKey());
            if (normalizedFeatureKey == null) {
                duplicateRows.add(override);
                continue;
            }
            OrgFeatureOverride previous = existingByFeatureKey.putIfAbsent(normalizedFeatureKey, override);
            if (previous != null) {
                duplicateRows.add(override);
            }
        }

        List<OrgFeatureOverride> toSave = new ArrayList<>();
        for (Map.Entry<String, FeatureState> entry : normalized.entrySet()) {
            String featureKey = entry.getKey();
            FeatureState state = entry.getValue();

            OrgFeatureOverride override = existingByFeatureKey.remove(featureKey);
            if (override == null) {
                override = OrgFeatureOverride.builder()
                        .organisation(org)
                        .featureKey(featureKey)
                        .enabled(state.enabled())
                        .usageLimit(state.usageLimit())
                        .build();
            } else {
                // Keep stored keys canonical to avoid case/whitespace drift.
                override.setFeatureKey(featureKey);
                override.setEnabled(state.enabled());
                override.setUsageLimit(state.usageLimit());
            }
            toSave.add(override);
        }

        List<OrgFeatureOverride> toDelete = new ArrayList<>(duplicateRows);
        toDelete.addAll(existingByFeatureKey.values());
        if (!toDelete.isEmpty()) {
            orgFeatureOverrideRepository.deleteAllInBatch(toDelete);
            orgFeatureOverrideRepository.flush();
        }
        if (!toSave.isEmpty()) {
            orgFeatureOverrideRepository.saveAll(toSave);
        }

        List<OrgFeatureOverride> after = orgFeatureOverrideRepository.findByOrganisationId(org.getId());
        platformAuditService.log(
                actorAuthId,
                "ORG_FEATURE_OVERRIDES_REPLACED",
                "Organisation",
                String.valueOf(org.getId()),
                toAuditJson(before, after)
        );

        evictFeatureCaches(org.getId());
        return getEffectiveFeatures(org.getId());
    }

    private Map<String, FeatureState> normalizeAndValidate(Map<String, FeatureState> requested) {
        LinkedHashMap<String, FeatureState> normalized = new LinkedHashMap<>();
        for (Map.Entry<String, FeatureState> entry : requested.entrySet()) {
            CoreFeature feature = CoreFeature.fromCode(entry.getKey());
            if (feature == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + entry.getKey());
            }
            if (entry.getValue() == null || entry.getValue().enabled() == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "enabled is required for feature " + feature.getCode());
            }
            if (feature.isToggleType() && entry.getValue().usageLimit() != null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "TOGGLE_KEY_HAS_LIMIT", "usageLimit is not allowed for " + feature.getCode());
            }
            if (feature.isLimitType()) {
                Integer usageLimit = entry.getValue().usageLimit();
                if (usageLimit == null) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_KEY_MISSING_LIMIT", "usageLimit is required for " + feature.getCode());
                }
                if (feature.getMinUsageLimit() != null && usageLimit < feature.getMinUsageLimit()) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_KEY_MISSING_LIMIT",
                            "usageLimit must be >= " + feature.getMinUsageLimit() + " for " + feature.getCode());
                }
            }
            normalized.put(feature.getCode(), entry.getValue());
        }
        return normalized;
    }

    private Organisation requireOrganisation(Long organisationId) {
        return organisationRepository.findById(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found"));
    }

    private String toAuditJson(List<OrgFeatureOverride> before, List<OrgFeatureOverride> after) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("before", toSnapshot(before));
        payload.put("after", toSnapshot(after));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }

    private Map<String, FeatureState> toSnapshot(List<OrgFeatureOverride> values) {
        LinkedHashMap<String, FeatureState> snapshot = new LinkedHashMap<>();
        for (OrgFeatureOverride value : values) {
            snapshot.put(value.getFeatureKey(), new FeatureState(Boolean.TRUE.equals(value.getEnabled()), value.getUsageLimit()));
        }
        return snapshot;
    }

    private void evictFeatureCaches(Long organisationId) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        try {
            RedisTemplate<String, Object> redis = redisTemplate.get();
            Set<String> keys = new HashSet<>();
            Set<String> orgKeys = redis.keys("feature:org:" + organisationId + ":*");
            if (orgKeys != null) {
                keys.addAll(orgKeys);
            }
            Set<String> planKeys = redis.keys("feature:plan:*");
            if (planKeys != null) {
                keys.addAll(planKeys);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ex) {
            if (redisFailOpen) {
                // Cache eviction is best-effort; do not fail business flow when Redis is down.
                log.warn("Feature cache eviction skipped because Redis is unavailable (organisationId={}): {}",
                        organisationId, ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    public record FeatureState(Boolean enabled, Integer usageLimit) {
    }

    private String normalizeFeatureKey(String featureKey) {
        CoreFeature coreFeature = CoreFeature.fromCode(featureKey);
        return coreFeature != null ? coreFeature.getCode() : null;
    }
}
