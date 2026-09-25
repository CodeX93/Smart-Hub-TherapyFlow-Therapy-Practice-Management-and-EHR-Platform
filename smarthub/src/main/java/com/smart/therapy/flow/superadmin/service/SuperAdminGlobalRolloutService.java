package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SuperAdminGlobalRolloutService {

    private final FeatureRolloutService featureRolloutService;
    private final PlatformAuditService platformAuditService;
    private final PlatformAuditLogRepository platformAuditLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<FeatureRolloutRule> listGlobalRollouts() {
        return featureRolloutService.listGlobalRules();
    }

    @Transactional(readOnly = true)
    public FeatureRolloutRule getGlobalRollout(Long ruleId) {
        return featureRolloutService.getGlobalRule(ruleId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Global rollout rule not found"));
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listGlobalRolloutHistory(int page, int size, String featureKey) {
        String normalizedFeatureKey = featureKey != null && !featureKey.isBlank()
                ? featureKey.trim().toUpperCase(Locale.ROOT)
                : null;
        return platformAuditLogRepository.findAll(
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .filter(log -> log.getAction() != null && log.getAction().startsWith("FEATURE_GLOBAL_ROLLOUT_"))
                .filter(log -> normalizedFeatureKey == null || containsFeatureKey(log.getDetails(), normalizedFeatureKey))
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listGlobalRolloutRuleHistory(Long ruleId, int page, int size) {
        String ruleIdText = String.valueOf(ruleId);
        return platformAuditLogRepository.findAll(
                        PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 100),
                                Sort.by(Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .filter(log -> log.getAction() != null && log.getAction().startsWith("FEATURE_GLOBAL_ROLLOUT_"))
                .filter(log -> ruleIdText.equals(log.getResourceId()))
                .map(this::toAuditLogResponse)
                .toList();
    }

    @Transactional
    public List<FeatureRolloutRule> upsertGlobalRollouts(List<RolloutRuleInput> rules, Long actorAuthId) {
        if (rules == null || rules.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "rules is required");
        }
        List<FeatureRolloutRule> saved = new ArrayList<>();
        for (RolloutRuleInput rule : rules) {
            FeatureRolloutRule savedRule = featureRolloutService.upsertGlobalRule(
                    rule.featureKey(),
                    Boolean.TRUE.equals(rule.enabled()),
                    rule.usageLimit(),
                    rule.startAt(),
                    rule.endAt()
            );
            saved.add(savedRule);
        }

        platformAuditService.log(
                actorAuthId,
                "FEATURE_GLOBAL_ROLLOUT_UPSERTED",
                "FeatureRolloutRule",
                "GLOBAL",
                "count=" + saved.size()
        );
        return saved;
    }

    @Transactional
    public void deleteGlobalRollout(Long ruleId, Long actorAuthId) {
        featureRolloutService.deleteGlobalRule(ruleId);
        platformAuditService.log(
                actorAuthId,
                "FEATURE_GLOBAL_ROLLOUT_DELETED",
                "FeatureRolloutRule",
                String.valueOf(ruleId),
                "scope=GLOBAL"
        );
    }

    @Transactional
    public List<FeatureRolloutRule> bulkDisableByFeatureKeys(List<String> featureKeys, Long actorAuthId) {
        if (featureKeys == null || featureKeys.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "featureKeys is required");
        }

        List<FeatureRolloutRule> saved = new ArrayList<>();
        for (String featureKey : featureKeys) {
            saved.add(featureRolloutService.upsertGlobalRule(
                    featureKey.trim().toUpperCase(Locale.ROOT),
                    false,
                    null,
                    null,
                    null
            ));
        }

        platformAuditService.log(
                actorAuthId,
                "FEATURE_GLOBAL_ROLLOUT_BULK_DISABLED",
                "FeatureRolloutRule",
                "GLOBAL",
                "count=" + saved.size()
        );
        return saved;
    }

    @Transactional
    public long bulkRemoveByFeatureKeys(List<String> featureKeys, Long actorAuthId) {
        if (featureKeys == null || featureKeys.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "featureKeys is required");
        }
        long deleted = 0;
        for (String featureKey : featureKeys) {
            deleted += featureRolloutService.deleteGlobalRulesByFeatureKey(featureKey);
        }
        platformAuditService.log(
                actorAuthId,
                "FEATURE_GLOBAL_ROLLOUT_BULK_REMOVED",
                "FeatureRolloutRule",
                "GLOBAL",
                "deleted=" + deleted
        );
        return deleted;
    }

    @Transactional
    public FeatureRolloutRule updateGlobalRollout(Long ruleId, RolloutRuleInput input, Long actorAuthId) {
        FeatureRolloutRule before = getGlobalRollout(ruleId);
        FeatureRolloutRule saved = featureRolloutService.updateGlobalRule(
                ruleId,
                input.featureKey(),
                input.enabled(),
                input.usageLimit(),
                input.startAt(),
                input.endAt()
        );
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "FEATURE_GLOBAL_ROLLOUT_UPDATED",
                "FeatureRolloutRule",
                String.valueOf(saved.getId()),
                rolloutSnapshot(before),
                rolloutSnapshot(saved),
                "featureKey=" + saved.getFeatureKey()
        );
        return saved;
    }

    private static Map<String, Object> rolloutSnapshot(FeatureRolloutRule rule) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", rule.getId());
        snapshot.put("scope", rule.getScope() != null ? rule.getScope().name() : null);
        snapshot.put("featureKey", rule.getFeatureKey());
        snapshot.put("enabled", rule.getEnabled());
        snapshot.put("usageLimit", rule.getUsageLimit());
        snapshot.put("startAt", rule.getStartAt());
        snapshot.put("endAt", rule.getEndAt());
        snapshot.put("updatedAt", rule.getUpdatedAt());
        return snapshot;
    }

    private boolean containsFeatureKey(String details, String featureKey) {
        if (details == null || details.isBlank()) {
            return false;
        }
        String text = details.toUpperCase(Locale.ROOT);
        if (text.contains(featureKey)) {
            return true;
        }
        try {
            JsonNode node = objectMapper.readTree(details);
            return node.toString().toUpperCase(Locale.ROOT).contains(featureKey);
        } catch (Exception ignored) {
            return false;
        }
    }

    private AuditLogEntryResponse toAuditLogResponse(PlatformAuditLog log) {
        AuditLogEntryResponse response = new AuditLogEntryResponse();
        response.setId(log.getId());
        response.setAuthId(log.getAuthId());
        response.setAction(log.getAction());
        response.setResourceType(log.getResourceType());
        response.setResourceId(log.getResourceId());
        response.setDetails(log.getDetails());
        response.setCreatedAt(log.getCreatedAt());
        if (log.getDetails() != null && !log.getDetails().isBlank()) {
            try {
                JsonNode node = objectMapper.readTree(log.getDetails());
                if (node.has("before")) {
                    response.setBefore(objectMapper.convertValue(node.get("before"), Object.class));
                }
                if (node.has("after")) {
                    response.setAfter(objectMapper.convertValue(node.get("after"), Object.class));
                }
            } catch (Exception ignored) {
            }
        }
        return response;
    }

    public record RolloutRuleInput(
            String featureKey,
            Boolean enabled,
            Integer usageLimit,
            java.time.Instant startAt,
            java.time.Instant endAt
    ) {
    }
}
