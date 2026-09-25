package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import com.smart.therapy.flow.superadmin.repository.FeatureRolloutRuleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FeatureRolloutService {

    private final FeatureRolloutRuleRepository repository;
    private final OrganisationRepository organisationRepository;
    private final UserOrganisationRepository userOrganisationRepository;
    private final AppFeatureRepository appFeatureRepository;

    @Transactional(readOnly = true)
    public List<FeatureRolloutRule> listOrganisationRules(Long organisationId) {
        return repository.findAllByOrganisationId(organisationId);
    }

    @Transactional(readOnly = true)
    public List<FeatureRolloutRule> listGlobalRules() {
        return repository.findAllByScope(FeatureRolloutRule.Scope.GLOBAL);
    }

    @Transactional(readOnly = true)
    public Optional<FeatureRolloutRule> getGlobalRule(Long ruleId) {
        return repository.findByIdAndScope(ruleId, FeatureRolloutRule.Scope.GLOBAL);
    }

    @Transactional
    public FeatureRolloutRule upsertRule(Long organisationId,
                                         FeatureRolloutRule.Scope scope,
                                         Long targetId,
                                         String targetKey,
                                         String featureKey,
                                         boolean enabled,
                                         Integer usageLimit,
                                         Instant startAt,
                                         Instant endAt) {
        if (scope == FeatureRolloutRule.Scope.GLOBAL) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_SCOPE", "GLOBAL scope must use global rollout endpoint");
        }
        String normalizedFeatureKey = resolveAvailableFeatureKey(featureKey);
        if (usageLimit != null && usageLimit < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NEGATIVE_LIMIT", "usageLimit cannot be negative");
        }
        if (scope != FeatureRolloutRule.Scope.ORGANISATION && targetId == null && (targetKey == null || targetKey.isBlank())) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "MISSING_TARGET_ID", "targetId is required for THERAPIST scope");
        }
        if (scope == FeatureRolloutRule.Scope.ORGANISATION) {
            targetId = null;
            targetKey = null;
        } else if (targetId != null && !userOrganisationRepository.existsTherapistInOrganisation(targetId, organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "THERAPIST_NOT_FOUND", "Therapist not found in organisation");
        }
        Instant now = Instant.now();
        Instant effectiveStart = startAt != null ? startAt : now;
        if (endAt != null && !endAt.isAfter(effectiveStart)) {
            throw new BadRequestException("endAt must be after startAt");
        }

        Organisation organisation = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "ORG_NOT_FOUND", "Organisation not found: " + organisationId));

        String normalizedTarget = targetKey != null ? targetKey.trim() : null;

        Optional<FeatureRolloutRule> existing = targetId != null
                ? repository.findLatestActiveRule(organisationId, scope, targetId, normalizedFeatureKey, now)
                : repository.findLatestActiveRuleByTargetKey(organisationId, scope, normalizedTarget, normalizedFeatureKey, now);

        FeatureRolloutRule rule = existing.orElseGet(FeatureRolloutRule::new);
        rule.setOrganisation(organisation);
        rule.setScope(scope);
        rule.setTargetId(targetId);
        rule.setTargetKey(normalizedTarget);
        rule.setFeatureKey(normalizedFeatureKey);
        rule.setEnabled(enabled);
        rule.setUsageLimit(usageLimit);
        rule.setStartAt(effectiveStart);
        rule.setEndAt(endAt);
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        rule.setUpdatedAt(now);
        return repository.save(rule);
    }

    @Transactional
    public FeatureRolloutRule upsertGlobalRule(String featureKey,
                                               boolean enabled,
                                               Integer usageLimit,
                                               Instant startAt,
                                               Instant endAt) {
        String normalizedFeatureKey = resolveAvailableFeatureKey(featureKey);
        if (usageLimit != null && usageLimit < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NEGATIVE_LIMIT", "usageLimit cannot be negative");
        }

        Instant now = Instant.now();
        Instant effectiveStart = startAt != null ? startAt : now;
        if (endAt != null && !endAt.isAfter(effectiveStart)) {
            throw new BadRequestException("endAt must be after startAt");
        }

        Optional<FeatureRolloutRule> existing = repository.findLatestActiveGlobalRule(
                FeatureRolloutRule.Scope.GLOBAL,
                normalizedFeatureKey,
                now
        );

        FeatureRolloutRule rule = existing.orElseGet(FeatureRolloutRule::new);
        rule.setOrganisation(null);
        rule.setScope(FeatureRolloutRule.Scope.GLOBAL);
        rule.setTargetId(null);
        rule.setTargetKey(null);
        rule.setFeatureKey(normalizedFeatureKey);
        rule.setEnabled(enabled);
        rule.setUsageLimit(usageLimit);
        rule.setStartAt(effectiveStart);
        rule.setEndAt(endAt);
        if (rule.getCreatedAt() == null) {
            rule.setCreatedAt(now);
        }
        rule.setUpdatedAt(now);
        return repository.save(rule);
    }

    @Transactional
    public FeatureRolloutRule updateGlobalRule(Long ruleId,
                                               String featureKey,
                                               Boolean enabled,
                                               Integer usageLimit,
                                               Instant startAt,
                                               Instant endAt) {
        FeatureRolloutRule rule = repository.findByIdAndScope(ruleId, FeatureRolloutRule.Scope.GLOBAL)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Global rollout rule not found"));

        String effectiveFeatureKey = featureKey != null ? featureKey : rule.getFeatureKey();
        String normalizedFeatureKey = resolveAvailableFeatureKey(effectiveFeatureKey);
        if (usageLimit != null && usageLimit < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NEGATIVE_LIMIT", "usageLimit cannot be negative");
        }

        Instant effectiveStart = startAt != null ? startAt : rule.getStartAt();
        Instant effectiveEnd = endAt != null ? endAt : rule.getEndAt();
        if (effectiveEnd != null && !effectiveEnd.isAfter(effectiveStart)) {
            throw new BadRequestException("endAt must be after startAt");
        }

        rule.setFeatureKey(normalizedFeatureKey);
        if (enabled != null) {
            rule.setEnabled(enabled);
        }
        if (usageLimit != null) {
            rule.setUsageLimit(usageLimit);
        }
        if (startAt != null) {
            rule.setStartAt(startAt);
        }
        if (endAt != null) {
            rule.setEndAt(endAt);
        }
        rule.setUpdatedAt(Instant.now());
        return repository.save(rule);
    }

    @Transactional(readOnly = true)
    public Optional<FeatureRolloutRule> findActiveOrganisationRule(Long organisationId, String featureKey, Instant at) {
        if (organisationId == null || featureKey == null || featureKey.isBlank()) {
            return Optional.empty();
        }
        Instant when = at != null ? at : Instant.now();
        return repository.findLatestActiveRule(
                organisationId,
                FeatureRolloutRule.Scope.ORGANISATION,
                null,
                featureKey.trim().toUpperCase(Locale.ROOT),
                when
        );
    }

    @Transactional(readOnly = true)
    public Optional<FeatureRolloutRule> findActiveGlobalRule(String featureKey, Instant at) {
        if (featureKey == null || featureKey.isBlank()) {
            return Optional.empty();
        }
        Instant when = at != null ? at : Instant.now();
        return repository.findLatestActiveGlobalRule(
                FeatureRolloutRule.Scope.GLOBAL,
                featureKey.trim().toUpperCase(Locale.ROOT),
                when
        );
    }

    @Transactional(readOnly = true)
    public Optional<FeatureRolloutRule> findActiveTargetRule(Long organisationId, Long targetId, String featureKey, Instant at) {
        if (organisationId == null || featureKey == null || featureKey.isBlank() || targetId == null) {
            return Optional.empty();
        }
        Instant when = at != null ? at : Instant.now();
        return repository.findLatestActiveRule(
                organisationId,
                FeatureRolloutRule.Scope.THERAPIST,
                targetId,
                featureKey.trim().toUpperCase(Locale.ROOT),
                when
        );
    }

    @Transactional(readOnly = true)
    public Optional<FeatureRolloutRule> findActiveTargetRule(Long organisationId, String targetKey, String featureKey, Instant at) {
        if (organisationId == null || featureKey == null || featureKey.isBlank() || targetKey == null || targetKey.isBlank()) {
            return Optional.empty();
        }
        Instant when = at != null ? at : Instant.now();
        return repository.findLatestActiveRuleByTargetKey(
                organisationId,
                FeatureRolloutRule.Scope.THERAPIST,
                targetKey.trim(),
                featureKey.trim().toUpperCase(Locale.ROOT),
                when
        );
    }

    @Transactional
    public void deleteRule(Long organisationId, Long ruleId) {
        FeatureRolloutRule rule = repository.findByIdAndOrganisationId(ruleId, organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Rollout rule not found"));
        repository.delete(rule);
    }

    @Transactional
    public void deleteGlobalRule(Long ruleId) {
        FeatureRolloutRule rule = repository.findById(ruleId)
                .filter(r -> r.getScope() == FeatureRolloutRule.Scope.GLOBAL)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Global rollout rule not found"));
        repository.delete(rule);
    }

    @Transactional
    public long deleteGlobalRulesByFeatureKey(String featureKey) {
        String normalized = resolveAvailableFeatureKey(featureKey);
        return repository.deleteByScopeAndFeatureKey(FeatureRolloutRule.Scope.GLOBAL, normalized);
    }

    private String resolveAvailableFeatureKey(String featureKey) {
        if (featureKey == null || featureKey.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + featureKey);
        }
        String normalized = featureKey.trim().toUpperCase(Locale.ROOT);
        AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(normalized)
                .orElseThrow(() -> new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + featureKey));
        if (Boolean.TRUE.equals(feature.getIsDeleted()) || Boolean.TRUE.equals(feature.getIsDeprecated())) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + featureKey);
        }
        return feature.getCode() != null ? feature.getCode().trim().toUpperCase(Locale.ROOT) : normalized;
    }
}
