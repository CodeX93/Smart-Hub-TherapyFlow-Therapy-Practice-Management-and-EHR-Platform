package com.smart.therapy.flow.superadmin.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.AppFeature;
import com.smart.therapy.flow.subscription.entity.PlanFeatureVersion;
import com.smart.therapy.flow.subscription.entity.PlanPricingTier;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.feature.FeatureType;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import com.smart.therapy.flow.subscription.repository.PlanPricingTierRepository;
import com.smart.therapy.flow.subscription.repository.PlanFeatureVersionRepository;
import com.smart.therapy.flow.subscription.repository.SubscriptionPlanRepository;
import com.smart.therapy.flow.superadmin.dto.AdminPlanEntitlementsRequest;
import com.smart.therapy.flow.superadmin.dto.PlanEntitlementItemResponse;
import com.smart.therapy.flow.superadmin.dto.PlanEntitlementResponse;
import com.smart.therapy.flow.superadmin.dto.PlanEntitlementsImportResponse;
import com.smart.therapy.flow.superadmin.dto.PlanEntitlementsResponse;
import com.smart.therapy.flow.superadmin.dto.PlanPricingTierRequest;
import com.smart.therapy.flow.superadmin.dto.PlanPricingTierResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminPlanCatalogService {

    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final AppFeatureRepository appFeatureRepository;
    private final PlanFeatureVersionRepository planFeatureVersionRepository;
    private final PlanPricingTierRepository planPricingTierRepository;
    private final PlatformAuditService platformAuditService;
    private final ObjectMapper objectMapper;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    @Value("${app.redis.fail-open:true}")
    private boolean redisFailOpen;

    @Transactional(readOnly = true)
    public List<SubscriptionPlan> listPlans() {
        return subscriptionPlanRepository.findAllByStatusOrderByBasePriceAsc(
                com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus.ACTIVE);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PlanPricingTierResponse.TierItem>> listPricingTiersForPlans(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<PlanPricingTier> tiers = planPricingTierRepository.findByPlanIdInOrderByPlanIdAscMinTherapistsAsc(planIds);
        Map<Long, List<PlanPricingTierResponse.TierItem>> grouped = new LinkedHashMap<>();
        for (PlanPricingTier tier : tiers) {
            PlanPricingTierResponse.TierItem item = new PlanPricingTierResponse.TierItem();
            item.setMinTherapists(tier.getMinTherapists());
            item.setMaxTherapists(tier.getMaxTherapists());
            item.setPricePerTherapistUsd(tier.getPricePerTherapistUsd());
            item.setIncludedSupervisors(tier.getIncludedSupervisors());
            item.setIncludedClients(tier.getIncludedClients());
            grouped.computeIfAbsent(tier.getPlan().getId(), key -> new ArrayList<>()).add(item);
        }
        return grouped;
    }

    @Transactional
    public SubscriptionPlan createPlan(String code,
                                       String name,
                                       String description,
                                       BigDecimal basePrice,
                                       BigDecimal annualPrice,
                                       String billingCycle,
                                       Integer trialDays,
                                       String status,
                                       String providerPriceIdMonthly,
                                       String providerPriceIdAnnual,
                                       Long actorAuthId) {
        if (code == null || code.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "code is required");
        }
        if (name == null || name.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "name is required");
        }
        if (billingCycle == null || billingCycle.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "billingCycle is required");
        }
        if (basePrice == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "basePrice is required");
        }
        if (basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "basePrice must be >= 0");
        }
        if (annualPrice != null && annualPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "annualPrice must be >= 0");
        }
        if (trialDays != null && trialDays < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "trialDays must be >= 0");
        }

        String normalizedCode = normalizePlanCode(code);
        if (subscriptionPlanRepository.existsByCodeIgnoreCase(normalizedCode)) {
            throw new StoryApiException(HttpStatus.CONFLICT, "PLAN_CODE_EXISTS", "Plan code already exists");
        }
        if (subscriptionPlanRepository.findByNameIgnoreCase(name.trim()).isPresent()) {
            throw new StoryApiException(HttpStatus.CONFLICT, "PLAN_NAME_EXISTS", "Plan name already exists");
        }

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode(normalizedCode);
        plan.setName(name.trim());
        plan.setDescription(description);
        plan.setBasePrice(basePrice);
        plan.setAnnualPrice(annualPrice);
        plan.setBillingCycle(normalizeBillingCycle(billingCycle));
        plan.setTrialDays(trialDays);
        plan.setStatus(resolveStatus(status));
        plan.setProviderPriceIdMonthly(normalizePriceId(providerPriceIdMonthly));
        plan.setProviderPriceIdAnnual(normalizePriceId(providerPriceIdAnnual));

        plan = subscriptionPlanRepository.save(plan);
        Map<String, Object> after = planSnapshot(plan);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_CREATED",
                "SubscriptionPlan",
                plan.getCode(),
                null,
                after,
                "name=" + plan.getName() + ", billingCycle=" + plan.getBillingCycle()
        );
        return plan;
    }

    @Transactional(readOnly = true)
    public SubscriptionPlan getPlanByCodeOrName(String codeOrName) {
        if (codeOrName == null || codeOrName.isBlank()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "plan identifier is required");
        }
        SubscriptionPlan plan = subscriptionPlanRepository.findByCodeIgnoreCase(codeOrName.trim()).orElse(null);
        if (plan == null) {
            plan = subscriptionPlanRepository.findByNameIgnoreCase(codeOrName.trim()).orElse(null);
        }
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + codeOrName);
        }
        return plan;
    }

    @Transactional
    public SubscriptionPlan updatePlanDetails(String codeOrName,
                                              String name,
                                              String description,
                                              BigDecimal basePrice,
                                              BigDecimal annualPrice,
                                              String billingCycle,
                                              Integer trialDays,
                                              String status,
                                              String providerPriceIdMonthly,
                                              String providerPriceIdAnnual,
                                              Long actorAuthId) {
        SubscriptionPlan plan = getPlanByCodeOrName(codeOrName);
        Map<String, Object> before = planSnapshot(plan);
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "basePrice must be >= 0");
        }
        if (annualPrice != null && annualPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "annualPrice must be >= 0");
        }
        if (trialDays != null && trialDays < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "trialDays must be >= 0");
        }
        if (name != null && !name.isBlank()) {
            var existing = subscriptionPlanRepository.findByNameIgnoreCase(name.trim()).orElse(null);
            if (existing != null && !existing.getId().equals(plan.getId())) {
                throw new StoryApiException(HttpStatus.CONFLICT, "PLAN_NAME_EXISTS", "Plan name already exists");
            }
            plan.setName(name.trim());
        }
        if (description != null) {
            plan.setDescription(description);
        }
        if (basePrice != null) {
            plan.setBasePrice(basePrice);
        }
        if (annualPrice != null) {
            plan.setAnnualPrice(annualPrice);
        }
        if (billingCycle != null && !billingCycle.isBlank()) {
            plan.setBillingCycle(normalizeBillingCycle(billingCycle));
        }
        if (trialDays != null) {
            plan.setTrialDays(trialDays);
        }
        if (status != null && !status.isBlank()) {
            plan.setStatus(resolveStatus(status));
        }
        if (providerPriceIdMonthly != null) {
            plan.setProviderPriceIdMonthly(normalizePriceId(providerPriceIdMonthly));
        }
        if (providerPriceIdAnnual != null) {
            plan.setProviderPriceIdAnnual(normalizePriceId(providerPriceIdAnnual));
        }

        plan = subscriptionPlanRepository.save(plan);
        Map<String, Object> after = planSnapshot(plan);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_UPDATED",
                "SubscriptionPlan",
                plan.getCode(),
                before,
                after,
                "basePrice=" + plan.getBasePrice() + ", annualPrice=" + plan.getAnnualPrice() + ", billingCycle=" + plan.getBillingCycle()
        );
        return plan;
    }

    @Transactional
    public SubscriptionPlan archivePlan(String codeOrName, Long actorAuthId) {
        SubscriptionPlan plan = getPlanByCodeOrName(codeOrName);
        if (plan.getStatus() == com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus.ARCHIVED) {
            return plan;
        }
        Map<String, Object> before = planSnapshot(plan);
        plan.setStatus(com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus.ARCHIVED);
        plan = subscriptionPlanRepository.save(plan);
        Map<String, Object> after = planSnapshot(plan);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_ARCHIVED",
                "SubscriptionPlan",
                plan.getCode(),
                before,
                after,
                "name=" + plan.getName()
        );
        return plan;
    }

    @Transactional
    public UpdatePlanResult updatePlan(String planName,
                                       String description,
                                       BigDecimal basePrice,
                                       BigDecimal annualPrice,
                                       String billingCycle,
                                       Integer trialDays,
                                       String status,
                                       Long actorAuthId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByCodeIgnoreCase(planName).orElse(null);
        if (plan == null) {
            plan = subscriptionPlanRepository.findByNameIgnoreCase(planName).orElse(null);
        }
        if (plan == null) {
            return UpdatePlanResult.error("Unknown plan: " + planName);
        }
        Map<String, Object> before = planSnapshot(plan);
        if (basePrice != null && basePrice.compareTo(BigDecimal.ZERO) < 0) {
            return UpdatePlanResult.error("basePrice must be >= 0");
        }
        if (annualPrice != null && annualPrice.compareTo(BigDecimal.ZERO) < 0) {
            return UpdatePlanResult.error("annualPrice must be >= 0");
        }
        if (billingCycle != null && !billingCycle.isBlank()) {
            String cycle = billingCycle.trim().toLowerCase(Locale.ROOT);
            if (!Set.of("monthly", "yearly", "annual").contains(cycle)) {
                return UpdatePlanResult.error("billingCycle must be monthly or yearly");
            }
            plan.setBillingCycle("annual".equals(cycle) ? "yearly" : cycle);
        }
        if (description != null) {
            plan.setDescription(description);
        }
        if (basePrice != null) {
            plan.setBasePrice(basePrice);
        }
        if (annualPrice != null) {
            plan.setAnnualPrice(annualPrice);
        }
        if (trialDays != null) {
            if (trialDays < 0) {
                return UpdatePlanResult.error("trialDays must be >= 0");
            }
            plan.setTrialDays(trialDays);
        }
        if (status != null && !status.isBlank()) {
            try {
                plan.setStatus(resolveStatus(status));
            } catch (Exception ex) {
                return UpdatePlanResult.error("status is invalid");
            }
        }

        plan = subscriptionPlanRepository.save(plan);
        Map<String, Object> after = planSnapshot(plan);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_UPDATED",
                "SubscriptionPlan",
                plan.getName(),
                before,
                after,
                "basePrice=" + plan.getBasePrice() + ", annualPrice=" + plan.getAnnualPrice() + ", billingCycle=" + plan.getBillingCycle()
        );
        return UpdatePlanResult.success(plan);
    }

    @Transactional
    public UpdateEntitlementsResult updateEntitlements(String planName,
                                                       List<EntitlementInput> items,
                                                       Long actorAuthId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(planName).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + planName);
        }
        if (items == null || items.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "features is required");
        }

        List<EntitlementInput> normalizedInputs = new ArrayList<>();
        for (EntitlementInput item : items) {
            if (item.featureCode() == null || item.featureCode().isBlank()) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "feature key is required");
            }
            CoreFeature feature = CoreFeature.fromCode(item.featureCode());
            if (feature == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + item.featureCode());
            }
            if (feature.isToggleType() && item.usageLimit() != null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "TOGGLE_KEY_HAS_LIMIT", "usageLimit is not allowed for " + feature.getCode());
            }
            if (feature.isLimitType()) {
                if (item.usageLimit() == null) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_MISSING", "usageLimit is required for " + feature.getCode());
                }
                if (feature.getMinUsageLimit() != null && item.usageLimit() < feature.getMinUsageLimit()) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_MISSING",
                            "usageLimit must be >= " + feature.getMinUsageLimit() + " for " + feature.getCode());
                }
            }
            normalizedInputs.add(new EntitlementInput(
                    feature.getCode(),
                    item.enabled(),
                    item.usageLimit(),
                    item.trialAvailable()
            ));
        }

        List<PlanFeatureVersion> before = planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(plan.getId());
        Instant now = Instant.now();
        List<PlanFeatureVersion> updated = new ArrayList<>();
        planFeatureVersionRepository.deleteByPlanId(plan.getId());

        for (EntitlementInput item : normalizedInputs) {
            AppFeature feature = ensureAppFeature(item.featureCode());
            PlanFeatureVersion next = new PlanFeatureVersion();
            next.setPlan(plan);
            next.setFeature(feature);
            next.setIsEnabled(item.enabled() == null || item.enabled());
            next.setUsageLimit(item.usageLimit());
            next.setIsTrialAvailable(Boolean.TRUE.equals(item.trialAvailable()));
            next.setEffectiveFrom(now);
            next.setEffectiveTo(null);
            next = planFeatureVersionRepository.save(next);
            updated.add(next);
        }

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_ENTITLEMENTS_UPDATED",
                "SubscriptionPlan",
                plan.getName(),
                snapshot(before),
                snapshot(updated),
                "source=legacy_entitlements"
        );
        evictPlanCaches(plan.getName());
        return UpdateEntitlementsResult.success(plan, updated);
    }

    @Transactional
    public PlanEntitlementsResponse updateEntitlementsFromCatalog(String planCode,
                                                                  List<AdminPlanEntitlementsRequest.PlanFeatureItem> items,
                                                                  Long actorAuthId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(planCode).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + planCode);
        }
        if (items == null || items.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "features is required");
        }

        List<AdminPlanEntitlementsRequest.PlanFeatureItem> normalized = new ArrayList<>();
        for (AdminPlanEntitlementsRequest.PlanFeatureItem item : items) {
            if (item.getKey() == null || item.getKey().isBlank()) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "feature key is required");
            }
            String key = item.getKey().trim();
            AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(key).orElse(null);
            if (feature == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + key);
            }
            CoreFeature core = CoreFeature.fromCode(feature.getCode());
            Integer normalizedLimit = item.getLimit();
            if (core != null) {
                if (core.isToggleType()) {
                    // Frontends may send a blanket limit for all features; toggle features ignore it.
                    normalizedLimit = null;
                }
                if (core.isLimitType()) {
                    if (normalizedLimit == null) {
                        throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_MISSING", "limit is required for " + core.getCode());
                    }
                    if (core.getMinUsageLimit() != null && normalizedLimit < core.getMinUsageLimit()) {
                        throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_MISSING",
                                "limit must be >= " + core.getMinUsageLimit() + " for " + core.getCode());
                    }
                }
            } else {
                if (normalizedLimit != null && normalizedLimit < 0) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "LIMIT_INVALID", "limit must be >= 0");
                }
            }
            if (item.getEnabled() == null) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "enabled is required");
            }
            AdminPlanEntitlementsRequest.PlanFeatureItem normalizedItem = new AdminPlanEntitlementsRequest.PlanFeatureItem();
            normalizedItem.setKey(key);
            normalizedItem.setEnabled(item.getEnabled());
            normalizedItem.setLimit(normalizedLimit);
            normalized.add(normalizedItem);
        }

        List<PlanFeatureVersion> before = planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(plan.getId());
        Instant now = Instant.now();
        planFeatureVersionRepository.deleteByPlanId(plan.getId());

        List<PlanFeatureVersion> updated = new ArrayList<>();
        for (AdminPlanEntitlementsRequest.PlanFeatureItem item : normalized) {
            AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(item.getKey().trim()).orElseThrow();
            PlanFeatureVersion next = new PlanFeatureVersion();
            next.setPlan(plan);
            next.setFeature(feature);
            next.setIsEnabled(Boolean.TRUE.equals(item.getEnabled()));
            next.setUsageLimit(item.getLimit());
            CoreFeature core = CoreFeature.fromCode(feature.getCode());
            next.setIsTrialAvailable(core != null && core.isToggleType()
                    ? Boolean.TRUE.equals(item.getEnabled())
                    : false);
            next.setEffectiveFrom(now);
            next.setEffectiveTo(null);
            updated.add(planFeatureVersionRepository.save(next));
        }

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_ENTITLEMENTS_UPDATED",
                "SubscriptionPlan",
                plan.getName(),
                snapshot(before),
                snapshot(updated),
                "source=catalog"
        );
        evictPlanCaches(plan.getName());

        List<PlanEntitlementItemResponse> features = updated.stream()
                .map(version -> PlanEntitlementItemResponse.builder()
                        .key(version.getFeature().getCode())
                        .enabled(Boolean.TRUE.equals(version.getIsEnabled()))
                        .limit(version.getUsageLimit())
                        .build())
                .toList();

        return PlanEntitlementsResponse.builder()
                .plan(plan.getName())
                .features(features)
                .updatedAt(now)
                .build();
    }

    @Transactional(readOnly = true)
    public PlanEntitlementsResponse getEntitlementsFromCatalog(String planCode) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(planCode).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + planCode);
        }
        List<PlanFeatureVersion> current = planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(plan.getId());
        List<PlanEntitlementItemResponse> features = current.stream()
                .map(version -> PlanEntitlementItemResponse.builder()
                        .key(version.getFeature().getCode())
                        .enabled(Boolean.TRUE.equals(version.getIsEnabled()))
                        .limit(version.getUsageLimit())
                        .build())
                .toList();
        return PlanEntitlementsResponse.builder()
                .plan(plan.getName())
                .features(features)
                .build();
    }

    @Transactional
    public PlanEntitlementsResponse applyEntitlementDefaults(String planName, Long actorAuthId) {
        String resolvedPlan = resolveDefaultPlanName(planName);
        DefaultPlanDefaults defaults = defaultsForPlan(resolvedPlan);
        if (defaults == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "DEFAULTS_NOT_DEFINED",
                    "No defaults defined for plan: " + planName);
        }

        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(resolvedPlan).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + resolvedPlan);
        }

        List<PlanFeatureVersion> current = planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(plan.getId());
        Map<String, AdminPlanEntitlementsRequest.PlanFeatureItem> merged = new LinkedHashMap<>();
        for (PlanFeatureVersion version : current) {
            AdminPlanEntitlementsRequest.PlanFeatureItem item = new AdminPlanEntitlementsRequest.PlanFeatureItem();
            item.setKey(version.getFeature().getCode());
            item.setEnabled(Boolean.TRUE.equals(version.getIsEnabled()));
            item.setLimit(version.getUsageLimit());
            merged.put(version.getFeature().getCode().trim().toUpperCase(Locale.ROOT), item);
        }

        for (DefaultEntitlement def : defaults.items()) {
            AppFeature feature = appFeatureRepository.findByCodeIgnoreCase(def.featureCode()).orElse(null);
            if (feature == null) {
                CoreFeature core = CoreFeature.fromCode(def.featureCode());
                if (core == null) {
                    continue;
                }
                feature = ensureAppFeature(core.getCode());
            }

            String key = feature.getCode().trim().toUpperCase(Locale.ROOT);
            AdminPlanEntitlementsRequest.PlanFeatureItem item = merged.getOrDefault(key, new AdminPlanEntitlementsRequest.PlanFeatureItem());
            item.setKey(feature.getCode());
            item.setEnabled(def.enabled());
            item.setLimit(def.limit());
            merged.put(key, item);
        }

        return updateEntitlementsFromCatalog(plan.getName(), new ArrayList<>(merged.values()), actorAuthId);
    }

    @Transactional
    public PlanPricingTierResponse replacePlanPricingTiers(String planCode,
                                                           List<PlanPricingTierRequest.TierItem> tiers,
                                                           Long actorAuthId) {
        if (tiers == null || tiers.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tiers is required");
        }
        SubscriptionPlan plan = getPlanByCodeOrName(planCode);

        List<PlanPricingTier> before = planPricingTierRepository.findByPlanIdOrderByMinTherapistsAsc(plan.getId());
        List<PlanPricingTierRequest.TierItem> normalized = tiers.stream()
                .map(this::normalizeTier)
                .sorted((a, b) -> Integer.compare(a.getMinTherapists(), b.getMinTherapists()))
                .toList();

        validateTierRanges(normalized);

        planPricingTierRepository.deleteByPlanId(plan.getId());
        List<PlanPricingTier> saved = new ArrayList<>();
        for (PlanPricingTierRequest.TierItem item : normalized) {
            PlanPricingTier tier = new PlanPricingTier();
            tier.setPlan(plan);
            tier.setMinTherapists(item.getMinTherapists());
            tier.setMaxTherapists(item.getMaxTherapists());
            tier.setPricePerTherapistUsd(item.getPricePerTherapistUsd());
            tier.setIncludedSupervisors(item.getIncludedSupervisors());
            tier.setIncludedClients(item.getIncludedClients());
            saved.add(planPricingTierRepository.save(tier));
        }

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "PLAN_PRICING_TIERS_UPDATED",
                "SubscriptionPlan",
                plan.getCode(),
                snapshotPricingTiers(before),
                snapshotPricingTiers(saved),
                "tiers=" + saved.size()
        );

        PlanPricingTierResponse response = new PlanPricingTierResponse();
        response.setPlanId(plan.getCode());
        List<PlanPricingTierResponse.TierItem> responseItems = saved.stream().map(tier -> {
            PlanPricingTierResponse.TierItem item = new PlanPricingTierResponse.TierItem();
            item.setMinTherapists(tier.getMinTherapists());
            item.setMaxTherapists(tier.getMaxTherapists());
            item.setPricePerTherapistUsd(tier.getPricePerTherapistUsd());
            item.setIncludedSupervisors(tier.getIncludedSupervisors());
            item.setIncludedClients(tier.getIncludedClients());
            return item;
        }).toList();
        response.setTiers(responseItems);
        return response;
    }

    @Transactional(readOnly = true)
    public PlanPricingTierResponse getPlanPricingTiers(String planCode) {
        SubscriptionPlan plan = getPlanByCodeOrName(planCode);
        List<PlanPricingTier> tiers = planPricingTierRepository.findByPlanIdOrderByMinTherapistsAsc(plan.getId());
        PlanPricingTierResponse response = new PlanPricingTierResponse();
        response.setPlanId(plan.getCode());
        List<PlanPricingTierResponse.TierItem> responseItems = tiers.stream().map(tier -> {
            PlanPricingTierResponse.TierItem item = new PlanPricingTierResponse.TierItem();
            item.setMinTherapists(tier.getMinTherapists());
            item.setMaxTherapists(tier.getMaxTherapists());
            item.setPricePerTherapistUsd(tier.getPricePerTherapistUsd());
            item.setIncludedSupervisors(tier.getIncludedSupervisors());
            item.setIncludedClients(tier.getIncludedClients());
            return item;
        }).toList();
        response.setTiers(responseItems);
        return response;
    }

    @Transactional(readOnly = true)
    public String exportEntitlementsCsv(String planCode) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(planCode).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + planCode);
        }
        List<PlanFeatureVersion> current = planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(plan.getId());
        StringBuilder csv = new StringBuilder("key,enabled,limit\n");
        for (PlanFeatureVersion version : current) {
            csv.append(safeCsv(version.getFeature().getCode())).append(',')
                    .append(Boolean.TRUE.equals(version.getIsEnabled()))
                    .append(',')
                    .append(version.getUsageLimit() != null ? version.getUsageLimit() : "")
                    .append('\n');
        }
        return csv.toString();
    }

    @Transactional
    public PlanEntitlementsImportResponse importEntitlementsCsv(String planCode, String csvContent, Long actorAuthId) {
        SubscriptionPlan plan = subscriptionPlanRepository.findByNameIgnoreCase(planCode).orElse(null);
        if (plan == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "PLAN_NOT_FOUND", "Unknown plan: " + planCode);
        }
        if (!org.springframework.util.StringUtils.hasText(csvContent)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSV content is required");
        }
        List<String> lines = csvContent.lines().toList();
        if (lines.isEmpty()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "CSV content is empty");
        }
        int startIndex = 0;
        List<String> header = parseCsvLine(lines.get(0));
        if (!header.isEmpty() && header.get(0).toLowerCase(Locale.ROOT).contains("key")) {
            startIndex = 1;
        }

        List<AdminPlanEntitlementsRequest.PlanFeatureItem> items = new ArrayList<>();
        List<PlanEntitlementsImportResponse.ImportError> errors = new ArrayList<>();
        for (int i = startIndex; i < lines.size(); i++) {
            String line = lines.get(i);
            if (line == null || line.isBlank()) {
                continue;
            }
            try {
                List<String> cols = parseCsvLine(line);
                if (cols.size() < 2) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Invalid CSV row");
                }
                AdminPlanEntitlementsRequest.PlanFeatureItem item = new AdminPlanEntitlementsRequest.PlanFeatureItem();
                item.setKey(cols.get(0));
                item.setEnabled(Boolean.parseBoolean(cols.get(1)));
                if (cols.size() > 2 && org.springframework.util.StringUtils.hasText(cols.get(2))) {
                    item.setLimit(Integer.parseInt(cols.get(2)));
                }
                items.add(item);
            } catch (Exception ex) {
                errors.add(PlanEntitlementsImportResponse.ImportError.builder()
                        .line(i + 1)
                        .error(ex.getMessage())
                        .build());
            }
        }

        if (!errors.isEmpty()) {
            return PlanEntitlementsImportResponse.builder()
                    .errors(errors)
                    .total(errors.size())
                    .build();
        }

        PlanEntitlementsResponse updated = updateEntitlementsFromCatalog(planCode, items, actorAuthId);
        return PlanEntitlementsImportResponse.builder()
                .updated(updated)
                .total(items.size())
                .build();
    }

    private static List<String> parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        if (line == null) {
            return result;
        }
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString().trim());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        result.add(current.toString().trim());
        return result;
    }

    private static String safeCsv(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\n") || escaped.contains("\"")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    @Transactional(readOnly = true)
    public List<PlanFeatureVersion> listCurrentEntitlements(Long planId) {
        return planFeatureVersionRepository.findByPlanIdAndEffectiveToIsNull(planId);
    }

    @Transactional(readOnly = true)
    public Map<Long, List<PlanEntitlementResponse>> listEntitlementsForPlans(List<Long> planIds) {
        if (planIds == null || planIds.isEmpty()) {
            return Map.of();
        }
        List<PlanFeatureVersion> versions = planFeatureVersionRepository.findByPlanIdInAndEffectiveToIsNull(planIds);
        Map<Long, List<PlanEntitlementResponse>> grouped = new LinkedHashMap<>();
        for (PlanFeatureVersion ent : versions) {
            PlanEntitlementResponse row = new PlanEntitlementResponse();
            row.setPlanName(ent.getPlan().getName());
            row.setFeatureCode(ent.getFeature().getCode());
            row.setEnabled(Boolean.TRUE.equals(ent.getIsEnabled()));
            row.setUsageLimit(ent.getUsageLimit());
            row.setTrialAvailable(Boolean.TRUE.equals(ent.getIsTrialAvailable()));
            row.setEffectiveFrom(ent.getEffectiveFrom());
            grouped.computeIfAbsent(ent.getPlan().getId(), key -> new ArrayList<>()).add(row);
        }
        return grouped;
    }

    private Map<String, Object> planSnapshot(SubscriptionPlan plan) {
        if (plan == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", plan.getId());
        snapshot.put("code", plan.getCode());
        snapshot.put("name", plan.getName());
        snapshot.put("description", plan.getDescription());
        snapshot.put("status", plan.getStatus() != null ? plan.getStatus().name() : null);
        snapshot.put("billingCycle", plan.getBillingCycle());
        snapshot.put("basePrice", plan.getBasePrice());
        snapshot.put("annualPrice", plan.getAnnualPrice());
        snapshot.put("trialDays", plan.getTrialDays());
        snapshot.put("providerPriceIdMonthly", plan.getProviderPriceIdMonthly());
        snapshot.put("providerPriceIdAnnual", plan.getProviderPriceIdAnnual());
        snapshot.put("updatedAt", plan.getUpdatedAt());
        return snapshot;
    }

    private AppFeature ensureAppFeature(String featureCode) {
        return appFeatureRepository.findByCode(featureCode).orElseGet(() -> {
            CoreFeature core = CoreFeature.fromCode(featureCode);
            AppFeature feature = AppFeature.builder()
                    .code(featureCode)
                    .name(featureCode.replace('_', ' '))
                    .description("Auto-created core feature")
                    .type(com.smart.therapy.flow.subscription.feature.FeatureType.CORE)
                    .scope(com.smart.therapy.flow.subscription.feature.FeatureScope.TENANT)
                    .defaultEnabled(core != null && core.getCatalogDefaultEnabled())
                    .build();
            return appFeatureRepository.save(feature);
        });
    }

    private String toAuditJson(List<PlanFeatureVersion> before, List<PlanFeatureVersion> after) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("before", snapshot(before));
        payload.put("after", snapshot(after));
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            return payload.toString();
        }
    }

    private List<Map<String, Object>> snapshot(List<PlanFeatureVersion> values) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (PlanFeatureVersion value : values) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("featureKey", value.getFeature().getCode());
            row.put("enabled", Boolean.TRUE.equals(value.getIsEnabled()));
            row.put("usageLimit", value.getUsageLimit());
            row.put("trialAvailable", Boolean.TRUE.equals(value.getIsTrialAvailable()));
            items.add(row);
        }
        return items;
    }

    private List<Map<String, Object>> snapshotPricingTiers(List<PlanPricingTier> tiers) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (PlanPricingTier tier : tiers) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("minTherapists", tier.getMinTherapists());
            row.put("maxTherapists", tier.getMaxTherapists());
            row.put("pricePerTherapistUsd", tier.getPricePerTherapistUsd());
            row.put("includedSupervisors", tier.getIncludedSupervisors());
            row.put("includedClients", tier.getIncludedClients());
            items.add(row);
        }
        return items;
    }

    private void evictPlanCaches(String planName) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        try {
            RedisTemplate<String, Object> redis = redisTemplate.get();
            Set<String> keys = new HashSet<>();
            Set<String> planKeys = redis.keys("feature:plan:" + planName + ":*");
            if (planKeys != null) {
                keys.addAll(planKeys);
            }
            Set<String> orgKeys = redis.keys("feature:org:*");
            if (orgKeys != null) {
                keys.addAll(orgKeys);
            }
            if (!keys.isEmpty()) {
                redis.delete(keys);
            }
        } catch (Exception ex) {
            if (redisFailOpen) {
                // Cache eviction is best-effort only; business write should not fail when Redis is down.
                log.warn("Plan cache eviction skipped because Redis is unavailable (planName={}): {}", planName, ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    private PlanPricingTierRequest.TierItem normalizeTier(PlanPricingTierRequest.TierItem item) {
        if (item.getMinTherapists() == null || item.getMaxTherapists() == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "minTherapists and maxTherapists are required");
        }
        if (item.getMinTherapists() > item.getMaxTherapists()) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "minTherapists must be <= maxTherapists");
        }
        if (item.getPricePerTherapistUsd() == null || item.getPricePerTherapistUsd().compareTo(BigDecimal.ZERO) < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "pricePerTherapistUsd must be >= 0");
        }
        if (item.getIncludedSupervisors() == null || item.getIncludedSupervisors() < 0
                || item.getIncludedClients() == null || item.getIncludedClients() < 0) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "included counts must be >= 0");
        }
        return item;
    }

    private void validateTierRanges(List<PlanPricingTierRequest.TierItem> tiers) {
        if (tiers.isEmpty()) {
            return;
        }
        int lastMax = -1;
        for (PlanPricingTierRequest.TierItem item : tiers) {
            if (item.getMinTherapists() <= lastMax) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "tiers overlap");
            }
            lastMax = item.getMaxTherapists();
        }
    }

    private static String normalizePlanCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private static String normalizePriceId(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String normalizeBillingCycle(String billingCycle) {
        String cycle = billingCycle.trim().toLowerCase(Locale.ROOT);
        if (!Set.of("monthly", "yearly", "annual").contains(cycle)) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "billingCycle must be monthly or yearly");
        }
        return "annual".equals(cycle) ? "yearly" : cycle;
    }

    private static com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus resolveStatus(String status) {
        com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus resolved =
                com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus.fromValue(status);
        if (resolved == null) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "status is invalid");
        }
        return resolved;
    }

    private static String resolveDefaultPlanName(String planName) {
        if (planName == null || planName.isBlank()) {
            return planName;
        }
        String normalized = planName.trim();
        if ("trial".equalsIgnoreCase(normalized)
                || "free".equalsIgnoreCase(normalized)
                || "basic".equalsIgnoreCase(normalized)) {
            return "Starter";
        }
        return normalized;
    }

    private static DefaultPlanDefaults defaultsForPlan(String planName) {
        if (planName == null) {
            return null;
        }
        if ("Starter".equalsIgnoreCase(planName)) {
            return DefaultPlanDefaults.basicDefaults();
        }
        if ("Professional".equalsIgnoreCase(planName)) {
            return DefaultPlanDefaults.professionalDefaults();
        }
        if ("Enterprise".equalsIgnoreCase(planName)) {
            return DefaultPlanDefaults.enterpriseDefaults();
        }
        return null;
    }

    public record EntitlementInput(
            String featureCode,
            Boolean enabled,
            Integer usageLimit,
            Boolean trialAvailable
    ) {}

    public record UpdateEntitlementsResult(boolean success, String error, SubscriptionPlan plan, List<PlanFeatureVersion> versions) {
        public static UpdateEntitlementsResult success(SubscriptionPlan plan, List<PlanFeatureVersion> versions) {
            return new UpdateEntitlementsResult(true, null, plan, versions);
        }
        public static UpdateEntitlementsResult error(String error) {
            return new UpdateEntitlementsResult(false, error, null, List.of());
        }
    }

    public record UpdatePlanResult(boolean success, String error, SubscriptionPlan plan) {
        public static UpdatePlanResult success(SubscriptionPlan plan) {
            return new UpdatePlanResult(true, null, plan);
        }
        public static UpdatePlanResult error(String error) {
            return new UpdatePlanResult(false, error, null);
        }
    }

    private record DefaultEntitlement(String featureCode, boolean enabled, Integer limit) {}

    private record DefaultPlanDefaults(List<DefaultEntitlement> items) {
        static DefaultPlanDefaults basicDefaults() {
            return new DefaultPlanDefaults(List.of(
                    new DefaultEntitlement(CoreFeature.CLIENT_PORTAL.getCode(), true, null),
                    new DefaultEntitlement(CoreFeature.AI_REPORTS_PER_MONTH.getCode(), false, 0),
                    new DefaultEntitlement(CoreFeature.STRIPE_PAYMENTS.getCode(), false, null),
                    new DefaultEntitlement(CoreFeature.DOCUMENT_UPLOAD_GB.getCode(), true, 10),
                    new DefaultEntitlement(CoreFeature.THERAPIST_LIMIT.getCode(), true, 3),
                    new DefaultEntitlement(CoreFeature.SUPERVISOR_LIMIT.getCode(), true, 1),
                    new DefaultEntitlement(CoreFeature.CLIENT_LIMIT.getCode(), true, 100)
            ));
        }

        static DefaultPlanDefaults professionalDefaults() {
            return new DefaultPlanDefaults(List.of(
                    new DefaultEntitlement(CoreFeature.CLIENT_PORTAL.getCode(), true, null),
                    new DefaultEntitlement(CoreFeature.AI_REPORTS_PER_MONTH.getCode(), true, 500),
                    new DefaultEntitlement(CoreFeature.STRIPE_PAYMENTS.getCode(), true, null),
                    new DefaultEntitlement(CoreFeature.DOCUMENT_UPLOAD_GB.getCode(), true, 100),
                    new DefaultEntitlement(CoreFeature.THERAPIST_LIMIT.getCode(), true, 10),
                    new DefaultEntitlement(CoreFeature.SUPERVISOR_LIMIT.getCode(), true, 3),
                    new DefaultEntitlement(CoreFeature.CLIENT_LIMIT.getCode(), true, 1000)
            ));
        }

        static DefaultPlanDefaults enterpriseDefaults() {
            return new DefaultPlanDefaults(List.of(
                    new DefaultEntitlement(CoreFeature.CLIENT_PORTAL.getCode(), true, null),
                    new DefaultEntitlement(CoreFeature.AI_REPORTS_PER_MONTH.getCode(), true, 5000),
                    new DefaultEntitlement(CoreFeature.STRIPE_PAYMENTS.getCode(), true, null),
                    new DefaultEntitlement(CoreFeature.DOCUMENT_UPLOAD_GB.getCode(), true, 500),
                    new DefaultEntitlement(CoreFeature.THERAPIST_LIMIT.getCode(), true, 50),
                    new DefaultEntitlement(CoreFeature.SUPERVISOR_LIMIT.getCode(), true, 10),
                    new DefaultEntitlement(CoreFeature.CLIENT_LIMIT.getCode(), true, 10000)
            ));
        }
    }
}
