package com.smart.therapy.flow.subscription.service;

import com.smart.therapy.flow.subscription.entity.*;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.repository.*;
import com.smart.therapy.flow.superadmin.service.FeatureRolloutService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smart.therapy.flow.common.exception.StoryApiException;
import jakarta.persistence.EntityManager;

import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Runtime feature resolution: resolve enabled features for an org from
 * current subscription + plan feature versions + add-on purchases.
 * Organisation never stores current plan — resolve from org_subscriptions.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionFeatureService {

    public static final String FEATURE_CLIENT_LIMIT = CoreFeature.CLIENT_LIMIT.getCode();
    public static final String FEATURE_THERAPIST_SEATS = CoreFeature.THERAPIST_LIMIT.getCode();
    public static final String FEATURE_SESSIONS_PER_MONTH = CoreFeature.SESSIONS_PER_MONTH.getCode();
    public static final String FEATURE_FORM_TEMPLATES = CoreFeature.FORM_TEMPLATES.getCode();
    public static final String FEATURE_CLIENT_PORTAL = CoreFeature.CLIENT_PORTAL.getCode();
    public static final String FEATURE_ROLES_PERMISSIONS = CoreFeature.ROLES_PERMISSIONS.getCode();
    public static final String FEATURE_BILLING_MODULE = CoreFeature.BILLING_MODULE.getCode();
    public static final String FEATURE_AUDIT_EXPORT = CoreFeature.AUDIT_EXPORT.getCode();
    public static final String FEATURE_ASSESSMENT_TEMPLATES = CoreFeature.ASSESSMENT_TEMPLATES.getCode();
    public static final String FEATURE_AI_REPORTS_PER_MONTH = CoreFeature.AI_REPORTS_PER_MONTH.getCode();
    public static final String FEATURE_AI_CONTENT_GENERATIONS_PER_MONTH = CoreFeature.AI_CONTENT_GENERATIONS_PER_MONTH.getCode();
    public static final String FEATURE_DOCUMENT_UPLOAD_GB = CoreFeature.DOCUMENT_UPLOAD_GB.getCode();
    public static final String FEATURE_SUPERVISOR_LIMIT = CoreFeature.SUPERVISOR_LIMIT.getCode();
    public static final String FEATURE_TASK_LIMIT = CoreFeature.TASK_LIMIT.getCode();
    public static final String FEATURE_ZOOM_SESSIONS_PER_MONTH = CoreFeature.ZOOM_SESSIONS_PER_MONTH.getCode();
    public static final String FEATURE_STRIPE_PAYMENTS = CoreFeature.STRIPE_PAYMENTS.getCode();

    private static final long BYTES_PER_GB = 1024L * 1024L * 1024L;

    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final PlanFeatureVersionRepository planFeatureVersionRepository;
    private final OrgFeaturePurchaseRepository orgFeaturePurchaseRepository;
    private final AppFeatureRepository appFeatureRepository;
    private final FeatureUsageRepository featureUsageRepository;
    private final OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    private final FeatureRolloutService featureRolloutService;
    private final EntityManager entityManager;

    /**
     * Get the current (active) subscription for an organisation.
     * end_at is null = current.
     */
    @Transactional(readOnly = true)
    public OrgSubscription getCurrentSubscription(Long organisationId) {
        return orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
    }

    /**
     * Resolve whether a feature is enabled for the org at the given time.
     * feature_enabled = plan_feature_enabled OR purchased_feature (then still check role permission in controller).
     */
    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long organisationId, String featureCode, Instant at) {
        return isFeatureEnabled(organisationId, (Long) null, featureCode, at);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long organisationId, Long targetId, String featureCode, Instant at) {
        return isFeatureEnabledInternal(organisationId, targetId, null, featureCode, at);
    }

    @Transactional(readOnly = true)
    public boolean isFeatureEnabled(Long organisationId, String targetKey, String featureCode, Instant at) {
        return isFeatureEnabledInternal(organisationId, null, targetKey, featureCode, at);
    }

    private boolean isFeatureEnabledInternal(Long organisationId,
                                             Long targetId,
                                             String targetKey,
                                             String featureCode,
                                             Instant at) {
        if (at == null) at = Instant.now();
        CoreFeature coreFeature = CoreFeature.fromCode(featureCode);
        boolean enabled = coreFeature != null && coreFeature.getCatalogDefaultEnabled();

        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub != null) {
            // Trial: only features with is_trial_available
            if (sub.isTrialing()) {
                enabled = isFeatureEnabledForTrial(sub, featureCode, at);
            } else {
                Set<String> enabledCodes = resolveEnabledFeatureCodes(sub, at);
                enabled = enabledCodes.contains(featureCode);
            }
        }

        var globalRollout = featureRolloutService.findActiveGlobalRule(featureCode, at);
        if (globalRollout.isPresent()) {
            enabled = Boolean.TRUE.equals(globalRollout.get().getEnabled());
        }

        var orgOverride = orgFeatureOverrideRepository.findByOrganisationIdAndFeatureKey(organisationId, featureCode.trim().toUpperCase());
        if (orgOverride.isPresent()) {
            enabled = Boolean.TRUE.equals(orgOverride.get().getEnabled());
        }

        var orgRollout = featureRolloutService.findActiveOrganisationRule(organisationId, featureCode, at);
        if (orgRollout.isPresent()) {
            enabled = Boolean.TRUE.equals(orgRollout.get().getEnabled());
        }

        if (targetId != null) {
            var targetOverride = featureRolloutService.findActiveTargetRule(organisationId, targetId, featureCode, at);
            if (targetOverride.isPresent()) {
                return Boolean.TRUE.equals(targetOverride.get().getEnabled());
            }
        }

        if (targetKey != null && !targetKey.isBlank()) {
            var targetOverride = featureRolloutService.findActiveTargetRule(organisationId, targetKey, featureCode, at);
            if (targetOverride.isPresent()) {
                return Boolean.TRUE.equals(targetOverride.get().getEnabled());
            }
        }

        return enabled;
    }

    /**
     * All enabled feature codes for the org at the given time (plan + add-ons).
     */
    @Transactional(readOnly = true)
    public Set<String> getEnabledFeatureCodes(Long organisationId, Instant at) {
        if (at == null) at = Instant.now();
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return Set.of();
        return resolveEnabledFeatureCodes(sub, at);
    }

    private Set<String> resolveEnabledFeatureCodes(OrgSubscription sub, Instant at) {
        Set<String> codes = new HashSet<>();

        // Plan features valid now
        List<PlanFeatureVersion> planFeatures = planFeatureVersionRepository.findByPlanIdEffectiveAt(sub.getPlan().getId(), at);
        for (PlanFeatureVersion pf : planFeatures) {
            if (Boolean.TRUE.equals(pf.getIsEnabled())) {
                codes.add(pf.getFeature().getCode());
            }
        }

        // Add-on purchases active at this time
        List<OrgFeaturePurchase> purchases = orgFeaturePurchaseRepository.findActiveBySubscriptionIdAt(sub.getId(), at);
        for (OrgFeaturePurchase p : purchases) {
            codes.add(p.getFeature().getCode());
        }

        return codes;
    }

    private boolean isFeatureEnabledForTrial(OrgSubscription sub, String featureCode, Instant at) {
        return resolveEnabledFeatureCodes(sub, at).contains(featureCode);
    }

    /**
     * Usage limit for a feature from plan (null = unlimited). Does not include add-on quantity.
     */
    @Transactional(readOnly = true)
    public Integer getPlanUsageLimit(Long organisationId, String featureCode, Instant at) {
        if (at == null) at = Instant.now();
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return null;
        List<PlanFeatureVersion> planFeatures = planFeatureVersionRepository.findByPlanIdEffectiveAt(sub.getPlan().getId(), at);
        for (PlanFeatureVersion pf : planFeatures) {
            if (pf.getFeature().getCode().equals(featureCode)) {
                return pf.getUsageLimit();
            }
        }
        return null;
    }

    /**
     * Effective limit = plan limit + add-on quantity. Null means unlimited.
     */
    @Transactional(readOnly = true)
    public Integer getEffectiveLimit(Long organisationId, String featureCode, Instant at) {
        return getEffectiveLimit(organisationId, (Long) null, featureCode, at);
    }

    @Transactional(readOnly = true)
    public Integer getEffectiveLimit(Long organisationId, Long targetId, String featureCode, Instant at) {
        return getEffectiveLimitInternal(organisationId, targetId, null, featureCode, at);
    }

    @Transactional(readOnly = true)
    public Integer getEffectiveLimit(Long organisationId, String targetKey, String featureCode, Instant at) {
        return getEffectiveLimitInternal(organisationId, null, targetKey, featureCode, at);
    }

    private Integer getEffectiveLimitInternal(Long organisationId,
                                              Long targetId,
                                              String targetKey,
                                              String featureCode,
                                              Instant at) {
        if (at == null) at = Instant.now();
        CoreFeature coreFeature = CoreFeature.fromCode(featureCode);
        if (coreFeature != null && coreFeature.isLimitType()
                && !isFeatureEnabledInternal(organisationId, targetId, targetKey, featureCode, at)) {
            return null;
        }
        Integer effective = coreFeature != null ? coreFeature.getCatalogDefaultUsageLimit() : null;

        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub != null) {
            Integer planLimit = getPlanUsageLimit(organisationId, featureCode, at);
            List<OrgFeaturePurchase> addOns = orgFeaturePurchaseRepository.findActiveBySubscriptionIdAt(sub.getId(), at);
            int addOnQty = 0;
            for (OrgFeaturePurchase p : addOns) {
                if (p.getFeature().getCode().equals(featureCode)) {
                    addOnQty += (p.getQuantity() != null ? p.getQuantity() : 0);
                }
            }

            if (planLimit == null && addOnQty == 0) {
                effective = null;
            } else if (planLimit == null) {
                effective = addOnQty;
            } else {
                effective = planLimit + addOnQty;
            }
        }

        var globalRollout = featureRolloutService.findActiveGlobalRule(featureCode, at);
        if (globalRollout.isPresent() && globalRollout.get().getUsageLimit() != null) {
            effective = globalRollout.get().getUsageLimit();
        }

        var orgOverride = orgFeatureOverrideRepository.findByOrganisationIdAndFeatureKey(organisationId, featureCode.trim().toUpperCase());
        if (orgOverride.isPresent() && orgOverride.get().getUsageLimit() != null) {
            effective = orgOverride.get().getUsageLimit();
        }

        var orgRollout = featureRolloutService.findActiveOrganisationRule(organisationId, featureCode, at);
        if (orgRollout.isPresent() && orgRollout.get().getUsageLimit() != null) {
            effective = orgRollout.get().getUsageLimit();
        }

        if (targetId != null) {
            var targetOverride = featureRolloutService.findActiveTargetRule(organisationId, targetId, featureCode, at);
            if (targetOverride.isPresent() && targetOverride.get().getUsageLimit() != null) {
                return targetOverride.get().getUsageLimit();
            }
        }

        if (targetKey != null && !targetKey.isBlank()) {
            var targetOverride = featureRolloutService.findActiveTargetRule(organisationId, targetKey, featureCode, at);
            if (targetOverride.isPresent() && targetOverride.get().getUsageLimit() != null) {
                return targetOverride.get().getUsageLimit();
            }
        }

        return effective;
    }

    /**
     * Current usage count for a metered feature (e.g. SESSIONS_PER_MONTH) in the given period.
     */
    @Transactional(readOnly = true)
    public long getCurrentUsageInPeriod(Long organisationId, String featureCode, Instant periodStart, Instant periodEnd) {
        return getCurrentUsageInPeriod(organisationId, null, featureCode, periodStart, periodEnd);
    }

    /**
     * Current usage count for a metered feature scoped to a target key.
     */
    @Transactional(readOnly = true)
    public long getCurrentUsageInPeriod(Long organisationId, String targetKey, String featureCode, Instant periodStart, Instant periodEnd) {
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return 0;
        AppFeature feature = appFeatureRepository.findByCode(featureCode).orElse(null);
        if (feature == null) return 0;
        if (targetKey == null || targetKey.isBlank()) {
            return featureUsageRepository.findBySubscriptionIdAndFeatureIdAndPeriodStart(sub.getId(), feature.getId(), periodStart)
                    .map(FeatureUsage::getUsageCount)
                    .orElse(0L);
        }
        return featureUsageRepository.findBySubscriptionIdAndFeatureIdAndPeriodStartAndTargetKey(sub.getId(), feature.getId(), periodStart, targetKey)
                .map(FeatureUsage::getUsageCount)
                .orElse(0L);
    }

    /**
     * Increment usage for the current month period. Creates row if missing. Call when creating a session etc.
     */
    @Transactional
    public void incrementUsage(Long organisationId, String featureCode, long delta) {
        incrementUsage(organisationId, null, featureCode, delta);
    }

    @Transactional
    public void incrementUsage(Long organisationId, String targetKey, String featureCode, long delta) {
        if (delta <= 0) return;
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return;
        AppFeature feature = appFeatureRepository.findByCode(featureCode).orElse(null);
        if (feature == null) return;
        Instant now = Instant.now();
        Instant periodStart = monthStartUtc(YearMonth.from(now.atZone(ZoneOffset.UTC)));
        Instant periodEnd = monthEndExclusiveUtc(YearMonth.from(now.atZone(ZoneOffset.UTC)));

        int updated = (targetKey == null || targetKey.isBlank())
                ? featureUsageRepository.incrementUsageCount(sub.getId(), feature.getId(), periodStart, delta)
                : featureUsageRepository.incrementUsageCountForTarget(sub.getId(), feature.getId(), periodStart, delta, targetKey);
        if (updated > 0) {
            return;
        }

        ensureUsageRowExists(sub, feature, periodStart, periodEnd, 0L, targetKey);
        if (targetKey == null || targetKey.isBlank()) {
            featureUsageRepository.incrementUsageCount(sub.getId(), feature.getId(), periodStart, delta);
        } else {
            featureUsageRepository.incrementUsageCountForTarget(sub.getId(), feature.getId(), periodStart, delta, targetKey);
        }
    }

    /**
     * Atomically consume metered usage for current month.
     * Returns false when the feature has reached its limit.
     */
    @Transactional
    public boolean tryConsumeUsage(Long organisationId, String featureCode, long delta) {
        return tryConsumeUsage(organisationId, null, featureCode, delta);
    }

    @Transactional
    public boolean tryConsumeUsage(Long organisationId, String targetKey, String featureCode, long delta) {
        if (delta <= 0) return true;
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return true;
        AppFeature feature = appFeatureRepository.findByCode(featureCode).orElse(null);
        if (feature == null) return true;

        YearMonth period = YearMonth.from(Instant.now().atZone(ZoneOffset.UTC));
        Instant periodStart = monthStartUtc(period);
        Instant periodEnd = monthEndExclusiveUtc(period);

        Integer limit = getEffectiveLimit(organisationId, targetKey, featureCode, periodStart);
        if (limit != null && limit < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Feature limit must be >= 0 for " + featureCode);
        }

        if (limit == null) {
            int updated = (targetKey == null || targetKey.isBlank())
                    ? featureUsageRepository.incrementUsageCount(sub.getId(), feature.getId(), periodStart, delta)
                    : featureUsageRepository.incrementUsageCountForTarget(sub.getId(), feature.getId(), periodStart, delta, targetKey);
            if (updated > 0) {
                return true;
            }
            ensureUsageRowExists(sub, feature, periodStart, periodEnd, 0L, targetKey);
            if (targetKey == null || targetKey.isBlank()) {
                featureUsageRepository.incrementUsageCount(sub.getId(), feature.getId(), periodStart, delta);
            } else {
                featureUsageRepository.incrementUsageCountForTarget(sub.getId(), feature.getId(), periodStart, delta, targetKey);
            }
            return true;
        }

        int updated = (targetKey == null || targetKey.isBlank())
                ? featureUsageRepository.incrementUsageCountIfBelowLimit(
                    sub.getId(), feature.getId(), periodStart, delta, limit.longValue())
                : featureUsageRepository.incrementUsageCountIfBelowLimitForTarget(
                    sub.getId(), feature.getId(), periodStart, delta, limit.longValue(), targetKey);
        if (updated > 0) {
            return true;
        }

        ensureUsageRowExists(sub, feature, periodStart, periodEnd, 0L, targetKey);
        updated = (targetKey == null || targetKey.isBlank())
                ? featureUsageRepository.incrementUsageCountIfBelowLimit(
                    sub.getId(), feature.getId(), periodStart, delta, limit.longValue())
                : featureUsageRepository.incrementUsageCountIfBelowLimitForTarget(
                    sub.getId(), feature.getId(), periodStart, delta, limit.longValue(), targetKey);
        return updated > 0;
    }

    /**
     * Atomically consume usage and throw business exception if exceeded.
     */
    @Transactional
    public void consumeUsageOrThrow(Long organisationId, String featureCode, long delta, String actionLabel) {
        boolean consumed = tryConsumeUsage(organisationId, null, featureCode, delta);
        if (!consumed) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "LIMIT_EXCEEDED",
                    actionLabel + " limit reached for current month");
        }
    }

    @Transactional
    public void consumeUsageOrThrow(Long organisationId, String targetKey, String featureCode, long delta, String actionLabel) {
        boolean consumed = tryConsumeUsage(organisationId, targetKey, featureCode, delta);
        if (!consumed) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "LIMIT_EXCEEDED",
                    actionLabel + " limit reached for current month");
        }
    }

    /**
     * Consume document storage usage (bytes) against DOCUMENT_UPLOAD_GB limit.
     * Usage is tracked in bytes; limit is stored in GB.
     */
    @Transactional
    public void consumeDocumentStorageOrThrow(Long organisationId, long bytes, String actionLabel) {
        if (organisationId == null || bytes <= 0) {
            return;
        }
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return;
        AppFeature feature = appFeatureRepository.findByCode(FEATURE_DOCUMENT_UPLOAD_GB).orElse(null);
        if (feature == null) return;

        YearMonth period = YearMonth.from(Instant.now().atZone(ZoneOffset.UTC));
        Instant periodStart = monthStartUtc(period);
        Instant periodEnd = monthEndExclusiveUtc(period);

        Integer limitGb = getEffectiveLimit(organisationId, FEATURE_DOCUMENT_UPLOAD_GB, periodStart);
        if (limitGb != null && limitGb < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "Document upload limit must be >= 0");
        }

        if (limitGb == null) {
            incrementUsage(organisationId, FEATURE_DOCUMENT_UPLOAD_GB, bytes);
            return;
        }

        long maxBytes = limitGb.longValue() * BYTES_PER_GB;
        int updated = featureUsageRepository.incrementUsageCountIfBelowLimit(
                sub.getId(), feature.getId(), periodStart, bytes, maxBytes);
        if (updated > 0) {
            return;
        }

        ensureUsageRowExists(sub, feature, periodStart, periodEnd, 0L, null);
        updated = featureUsageRepository.incrementUsageCountIfBelowLimit(
                sub.getId(), feature.getId(), periodStart, bytes, maxBytes);
        if (updated <= 0) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "LIMIT_EXCEEDED",
                    actionLabel + " limit reached for current month");
        }
    }

    @Transactional(readOnly = true)
    public List<FeatureUsage> listUsageForPeriod(Long organisationId, YearMonth period) {
        return listUsageForPeriod(organisationId, period, null);
    }

    @Transactional(readOnly = true)
    public List<FeatureUsage> listUsageForPeriod(Long organisationId, YearMonth period, String targetKey) {
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return List.of();
        Instant periodStart = monthStartUtc(period);
        if (targetKey == null || targetKey.isBlank()) {
            return featureUsageRepository.findBySubscriptionIdAndPeriodStartAndTargetKeyIsNull(sub.getId(), periodStart);
        }
        return featureUsageRepository.findBySubscriptionIdAndPeriodStartAndTargetKey(sub.getId(), periodStart, targetKey);
    }

    @Transactional(readOnly = true)
    public List<FeatureUsage> listUsageForPeriodTargets(Long organisationId, YearMonth period) {
        OrgSubscription sub = getCurrentSubscription(organisationId);
        if (sub == null) return List.of();
        Instant periodStart = monthStartUtc(period);
        return featureUsageRepository.findBySubscriptionIdAndPeriodStartAndTargetKeyIsNotNull(sub.getId(), periodStart);
    }

    /**
     * Reset usage counters for a month and seed missing rows for current active subscriptions.
     */
    @Transactional
    public int resetUsageForMonth(YearMonth period) {
        Instant periodStart = monthStartUtc(period);
        Instant periodEnd = monthEndExclusiveUtc(period);
        int resetCount = featureUsageRepository.resetUsageByPeriodStart(periodStart);

        List<OrgSubscription> activeSubs = orgSubscriptionRepository.findAllCurrentWithOrganisationAndPlan();
        for (OrgSubscription sub : activeSubs) {
            Long orgId = sub.getOrganisation().getId();
            for (CoreFeature coreFeature : CoreFeature.values()) {
                if (!coreFeature.isLimitType()) {
                    continue;
                }
                Integer limit = getEffectiveLimit(orgId, coreFeature.getCode(), periodStart);
                if (limit == null) {
                    continue;
                }
                AppFeature feature = appFeatureRepository.findByCode(coreFeature.getCode()).orElse(null);
                if (feature == null) {
                    continue;
                }
                ensureUsageRowExists(sub, feature, periodStart, periodEnd, 0L, null);
            }
        }
        return resetCount;
    }

    private void ensureUsageRowExists(OrgSubscription sub, AppFeature feature, Instant periodStart, Instant periodEnd, long initial, String targetKey) {
        if (usageRowExists(sub, feature, periodStart, targetKey)) {
            return;
        }

        FeatureUsage usage = new FeatureUsage();
        usage.setSubscription(sub);
        usage.setFeature(feature);
        usage.setUsageCount(initial);
        usage.setPeriodStart(periodStart);
        usage.setPeriodEnd(periodEnd);
        usage.setTargetKey(normalizeTargetKey(targetKey));
        try {
            featureUsageRepository.saveAndFlush(usage);
        } catch (DataIntegrityViolationException ex) {
            log.debug(
                    "Feature usage row already exists (subscription={}, feature={}, period={}, target={})",
                    sub.getId(),
                    feature.getId(),
                    periodStart,
                    targetKey);
            entityManager.detach(usage);
        }
    }

    private boolean usageRowExists(OrgSubscription sub, AppFeature feature, Instant periodStart, String targetKey) {
        if (targetKey == null || targetKey.isBlank()) {
            return featureUsageRepository.findBySubscriptionIdAndFeatureIdAndPeriodStart(
                    sub.getId(), feature.getId(), periodStart).isPresent();
        }
        return featureUsageRepository.findBySubscriptionIdAndFeatureIdAndPeriodStartAndTargetKey(
                sub.getId(), feature.getId(), periodStart, targetKey).isPresent();
    }

    private static String normalizeTargetKey(String targetKey) {
        return (targetKey == null || targetKey.isBlank()) ? null : targetKey;
    }

    private static Instant monthStartUtc(YearMonth ym) {
        return ym.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    private static Instant monthEndExclusiveUtc(YearMonth ym) {
        return ym.plusMonths(1).atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
