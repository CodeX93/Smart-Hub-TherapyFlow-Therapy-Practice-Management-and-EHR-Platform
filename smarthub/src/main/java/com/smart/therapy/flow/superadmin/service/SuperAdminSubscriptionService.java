package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.repository.AuthIdentityRoleRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.util.RoleName;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.OrgFeatureOverride;
import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.repository.OrgFeatureOverrideRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.repository.SubscriptionPlanRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.superadmin.entity.FeatureRolloutRule;
import com.smart.therapy.flow.billing.service.PlatformSubscriptionInvoiceService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.payment.service.StripeSubscriptionAdminService;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SuperAdminSubscriptionService {

    private final OrganisationRepository organisationRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final SubscriptionPlanRepository subscriptionPlanRepository;
    private final OrgFeatureOverrideRepository orgFeatureOverrideRepository;
    private final AuthIdentityRoleRepository authIdentityRoleRepository;
    private final ClientRepository clientRepository;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final PlatformAuditService platformAuditService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final FeatureRolloutService featureRolloutService;
    private final SubscriptionLifecycleService subscriptionLifecycleService;
    private final StripeSubscriptionAdminService stripeSubscriptionAdminService;
    private final PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;
    private final StripePlatformSubscriptionService stripePlatformSubscriptionService;

    @Transactional(readOnly = true)
    public boolean organisationExists(Long organisationId) {
        return organisationRepository.existsById(organisationId);
    }

    @Transactional(readOnly = true)
    public Optional<OrgSubscription> getCurrentSubscription(Long organisationId) {
        return orgSubscriptionRepository.findCurrentByOrganisationId(organisationId);
    }

    @Transactional(readOnly = true)
    public StrictSubscriptionView getStrictSubscription(Long organisationId) {
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No subscription record for organisation"));
        return buildStrictView(sub);
    }

    @Transactional(readOnly = true)
    public SubscriptionStatusView getSubscriptionStatus(Long organisationId) {
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No subscription record for organisation"));

        Instant now = Instant.now();
        String subStatus = sub.getStatus() != null ? sub.getStatus().trim().toLowerCase(Locale.ROOT) : "";
        String orgStatus = sub.getOrganisation() != null && sub.getOrganisation().getStatus() != null
                ? sub.getOrganisation().getStatus().trim().toUpperCase(Locale.ROOT)
                : "";

        boolean isCurrent = sub.getEndAt() == null;
        boolean isTrialExpired = sub.getTrialEndAt() != null && !sub.getTrialEndAt().isAfter(now);
        boolean isPastDue = "past_due".equals(subStatus) || "failed".equals(subStatus);
        boolean isCancelled = "cancelled".equals(subStatus);
        boolean orgRestricted = Set.of("LOCKED", "ARCHIVED", "DELETED").contains(orgStatus);
        boolean accessRestricted = isPastDue || isCancelled || orgRestricted;

        boolean providerCustomerConfigured = StringUtils.hasText(sub.getProviderCustomerId());
        boolean providerSubscriptionConfigured = StringUtils.hasText(sub.getProviderSubscriptionId());
        boolean providerBillingConfigured = providerCustomerConfigured && providerSubscriptionConfigured;
        String billingMode = providerBillingConfigured ? "provider_managed" : "manual_or_unconfigured";

        return new SubscriptionStatusView(
                sub.getOrganisation().getId(),
                sub.getId(),
                sub.getPlan() != null ? sub.getPlan().getName() : null,
                sub.getStatus(),
                sub.getOrganisation() != null ? sub.getOrganisation().getStatus() : null,
                sub.getStartAt(),
                sub.getEndAt(),
                sub.getTrialEndAt(),
                isCurrent,
                isTrialExpired,
                isPastDue,
                isCancelled,
                accessRestricted,
                providerCustomerConfigured,
                providerSubscriptionConfigured,
                providerBillingConfigured,
                billingMode
        );
    }

    @Transactional
    public StrictSubscriptionView updateStrictSubscription(Long organisationId,
                                                           String planName,
                                                           String billingCycle,
                                                           Integer trialDays,
                                                           Instant effectiveDate,
                                                           Boolean prorate,
                                                           UserLimitPatch userLimitPatch,
                                                           Boolean createRenewalInvoice,
                                                           Long actorAuthId) {
        OrgSubscription sub = resolveOrCreateCurrentSubscriptionForStrictUpdate(
                organisationId,
                planName,
                billingCycle,
                trialDays,
                effectiveDate,
                prorate,
                createRenewalInvoice,
                actorAuthId
        );

        Instant now = Instant.now();
        if (effectiveDate != null && effectiveDate.isBefore(now.minusSeconds(1))) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_EFFECTIVE_DATE", "effectiveDate must be in the future");
        }

        boolean scheduleChange = effectiveDate != null && effectiveDate.isAfter(now.plusSeconds(1));
        if (scheduleChange && trialDays != null) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRIAL_DAYS", "trialDays cannot be scheduled");
        }
        if (scheduleChange && Boolean.TRUE.equals(prorate)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PRORATION", "proration is only supported for immediate changes");
        }

        SubscriptionPlan targetPlan = null;
        if (planName != null && !planName.isBlank()) {
            targetPlan = resolvePlanByCodeOrName(planName.trim())
                    .orElseThrow(() -> new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND", "Unknown plan: " + planName));
        }

        if (scheduleChange) {
            if (targetPlan == null && (billingCycle == null || billingCycle.isBlank())) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "plan or billingCycle is required for scheduled change");
            }
            String resolvedCycle = normalizeIncomingBillingCycle(billingCycle, defaultBillingCycle(targetPlan != null ? targetPlan : sub.getPlan()));
            SubscriptionPlan planToApply = targetPlan != null ? targetPlan : sub.getPlan();

            sub.setEndAt(effectiveDate);
            orgSubscriptionRepository.save(sub);

            OrgSubscription next = new OrgSubscription();
            next.setOrganisation(sub.getOrganisation());
            next.setPlan(planToApply);
            next.setStartAt(effectiveDate);
            next.setEndAt(null);
            next.setCreatedAt(now);
            next.setBillingCycleAtTime(resolvedCycle);
            next.setPriceAtTime(resolvePrice(planToApply, resolvedCycle));
            next.setProviderCustomerId(sub.getProviderCustomerId());
            next.setProviderSubscriptionId(sub.getProviderSubscriptionId());
            next.setStatus("active");
            orgSubscriptionRepository.save(next);

            platformAuditService.logWithSnapshots(
                    actorAuthId,
                    "SUBSCRIPTION_SCHEDULED",
                    "Organisation",
                    String.valueOf(organisationId),
                    subscriptionSnapshot(sub),
                    subscriptionSnapshot(next),
                    "plan=" + planToApply.getName() + ", cycle=" + resolvedCycle + ", effectiveAt=" + effectiveDate
            );
            return buildStrictView(sub);
        }

        boolean planChange = targetPlan != null;
        boolean cycleChange = billingCycle != null && !billingCycle.isBlank();
        SubscriptionPlan planToApply = sub.getPlan();
        String resolvedCycle = sub.getBillingCycleAtTime();
        if (planChange || cycleChange) {
            planToApply = planChange ? targetPlan : sub.getPlan();
            String fallbackCycle = sub.getBillingCycleAtTime();
            if (fallbackCycle == null || fallbackCycle.isBlank()) {
                fallbackCycle = defaultBillingCycle(planToApply);
            }
            resolvedCycle = normalizeIncomingBillingCycle(billingCycle, fallbackCycle);
        }
        if (Boolean.TRUE.equals(prorate) && !(planChange || cycleChange)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_PRORATION",
                    "proration requires a plan or billingCycle change");
        }
        if (planChange || cycleChange) {
            String providerSubscriptionId = sub.getProviderSubscriptionId();
            if (StringUtils.hasText(providerSubscriptionId)) {
                String priceId = SubscriptionPlanPriceResolver.resolveProviderPriceId(planToApply, resolvedCycle);
                if (!StringUtils.hasText(priceId)) {
                    throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRICE_ID_MISSING",
                            "Provider price id required for Stripe update");
                }
                stripeSubscriptionAdminService.updateSubscriptionPrice(
                        organisationId,
                        providerSubscriptionId,
                        priceId,
                        Boolean.TRUE.equals(prorate)
                );
            } else if (Boolean.TRUE.equals(prorate)) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRORATION_NOT_SUPPORTED",
                        "Provider subscription id required for proration");
            }
        }
        if (planChange) {
            sub.setPlan(planToApply);
        }
        if (planChange || cycleChange) {
            sub.setBillingCycleAtTime(resolvedCycle);
            sub.setPriceAtTime(resolvePrice(planToApply, resolvedCycle));
        }

        if (trialDays != null) {
            if (!(trialDays == 14 || trialDays == 30)) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRIAL_DAYS", "trialDays must be 14 or 30");
            }
            sub.setStatus("trialing");
            sub.setTrialEndAt(Instant.now().plus(trialDays, ChronoUnit.DAYS));
            sub.setDunningAttemptCount(1);
            sub.setNextDunningAt(null);
            sub.setLastPaymentFailedAt(null);
            sub.setLastDunningAt(null);
            sub.setNotifiedTrial(false);
        }

        OrgSubscription beforeSnapshot = copySnapshot(sub);
        orgSubscriptionRepository.save(sub);
        upsertUserLimitOverrides(sub.getOrganisation(), userLimitPatch);

        if (Boolean.TRUE.equals(prorate)) {
            platformAuditService.log(
                    actorAuthId,
                    "SUBSCRIPTION_PRORATION_REQUESTED",
                    "Organisation",
                    String.valueOf(organisationId),
                    "providerSubscriptionId=" + sub.getProviderSubscriptionId()
            );
        }

        platformAuditService.logWithSnapshots(
                actorAuthId,
                "SUBSCRIPTION_UPDATED_STRICT",
                "Organisation",
                String.valueOf(organisationId),
                subscriptionSnapshot(beforeSnapshot),
                subscriptionSnapshot(sub),
                "plan=" + sub.getPlan().getName() + ", cycle=" + sub.getBillingCycleAtTime() + ", trialDays=" + trialDays + ", prorate=" + prorate
        );
        return buildStrictView(sub);
    }

    private OrgSubscription resolveOrCreateCurrentSubscriptionForStrictUpdate(Long organisationId,
                                                                               String planName,
                                                                               String billingCycle,
                                                                               Integer trialDays,
                                                                               Instant effectiveDate,
                                                                               Boolean prorate,
                                                                               Boolean createRenewalInvoice,
                                                                               Long actorAuthId) {
        Optional<OrgSubscription> currentOpt = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId);
        if (currentOpt.isPresent()) {
            return currentOpt.get();
        }

        if (effectiveDate != null && effectiveDate.isAfter(Instant.now().plusSeconds(1))) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND",
                    "No current subscription found. Use immediate update to renew before scheduling future changes.");
        }
        if (Boolean.TRUE.equals(prorate)) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PRORATION_NOT_SUPPORTED",
                    "Proration requires an existing current provider subscription");
        }

        List<OrgSubscription> history = orgSubscriptionRepository.findAllByOrganisationIdOrderByStartAtDesc(organisationId);
        if (history.isEmpty()) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND", "No subscription record for organisation");
        }

        OrgSubscription latest = history.get(0);
        SubscriptionPlan planToApply = latest.getPlan();
        if (planName != null && !planName.isBlank()) {
            planToApply = resolvePlanByCodeOrName(planName.trim())
                    .orElseThrow(() -> new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PLAN_NOT_FOUND", "Unknown plan: " + planName));
        }

        String baseCycle = latest.getBillingCycleAtTime();
        if (baseCycle == null || baseCycle.isBlank()) {
            baseCycle = defaultBillingCycle(planToApply);
        }
        String resolvedCycle = normalizeIncomingBillingCycle(billingCycle, baseCycle);
        Instant now = Instant.now();

        OrgSubscription renewed = new OrgSubscription();
        renewed.setOrganisation(latest.getOrganisation());
        renewed.setPlan(planToApply);
        renewed.setStartAt(now);
        renewed.setEndAt(null);
        renewed.setCreatedAt(now);
        renewed.setBillingCycleAtTime(resolvedCycle);
        renewed.setPriceAtTime(resolvePrice(planToApply, resolvedCycle));
        renewed.setProviderCustomerId(latest.getProviderCustomerId());
        renewed.setProviderSubscriptionId(latest.getProviderSubscriptionId());
        renewed.setStatus("active");

        if (trialDays != null) {
            if (!(trialDays == 14 || trialDays == 30)) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVALID_TRIAL_DAYS", "trialDays must be 14 or 30");
            }
            renewed.setStatus("trialing");
            renewed.setTrialEndAt(now.plus(trialDays, ChronoUnit.DAYS));
        }

        OrgSubscription saved = orgSubscriptionRepository.save(renewed);
        if (Boolean.TRUE.equals(createRenewalInvoice)) {
            platformSubscriptionInvoiceService.createRenewalInvoice(saved);
        }
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "SUBSCRIPTION_RENEWED_FROM_EXPIRED",
                "Organisation",
                String.valueOf(organisationId),
                subscriptionSnapshot(latest),
                subscriptionSnapshot(saved),
                "plan=" + saved.getPlan().getName() + ", cycle=" + saved.getBillingCycleAtTime()
        );
        return saved;
    }

    @Transactional
    public OrgSubscription provisionStripeSubscription(Long organisationId, Long actorAuthId) {
        if (!organisationRepository.existsById(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "ORGANISATION_NOT_FOUND", "Organisation not found");
        }
        return stripePlatformSubscriptionService.provisionStripeSubscription(organisationId, actorAuthId);
    }

    @Transactional
    public Invoice createManualRenewalInvoice(Long organisationId, Long actorAuthId) {
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "SUBSCRIPTION_NOT_FOUND",
                        "No current subscription found for this organisation"));
        if (SubscriptionPlanPriceResolver.isProviderManaged(
                sub.getPlan(), sub.getProviderCustomerId(), sub.getProviderSubscriptionId())) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_MANAGED_SUBSCRIPTION",
                    "Manual renewal invoices are only for manual_or_unconfigured organisations");
        }
        Invoice invoice = platformSubscriptionInvoiceService.createRenewalInvoice(sub);
        platformAuditService.log(actorAuthId, "MANUAL_RENEWAL_INVOICE_CREATED", "Organisation",
                String.valueOf(organisationId), "invoiceId=" + invoice.getId());
        return invoice;
    }

    @Transactional
    public UpsertSubscriptionResult upsertSubscription(Long organisationId,
                                                       String planName,
                                                       Integer trialDays,
                                                       String billingCycle,
                                                       String status,
                                                       String providerCustomerId,
                                                       String providerSubscriptionId,
                                                       Long actorAuthId) {
        Organisation organisation = organisationRepository.findById(organisationId).orElse(null);
        if (organisation == null) {
            return UpsertSubscriptionResult.notFoundResult();
        }
        if (planName == null || planName.isBlank()) {
            return UpsertSubscriptionResult.error("planName is required");
        }
        SubscriptionPlan plan = resolvePlanByCodeOrName(planName.trim()).orElse(null);
        if (plan == null) {
            return UpsertSubscriptionResult.error("Unknown plan: " + planName);
        }
        if (trialDays != null && !(trialDays == 14 || trialDays == 30)) {
            return UpsertSubscriptionResult.error("trialDays must be 14 or 30");
        }

        String cycle = billingCycle != null && !billingCycle.isBlank()
                ? billingCycle.trim().toLowerCase(Locale.ROOT)
                : defaultBillingCycle(plan);
        if (!Set.of("monthly", "yearly").contains(cycle)) {
            return UpsertSubscriptionResult.error("billingCycle must be monthly or yearly");
        }

        Instant now = Instant.now();
        OrgSubscription current = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
        if (current != null) {
            current.setEndAt(now);
            orgSubscriptionRepository.save(current);
        }

        OrgSubscription next = new OrgSubscription();
        next.setOrganisation(organisation);
        next.setPlan(plan);
        next.setStartAt(now);
        next.setEndAt(null);
        next.setCreatedAt(now);
        next.setBillingCycleAtTime(cycle);
        next.setPriceAtTime(resolvePrice(plan, cycle));
        next.setProviderCustomerId(providerCustomerId);
        next.setProviderSubscriptionId(providerSubscriptionId);

        if (trialDays != null) {
            next.setStatus("trialing");
            next.setTrialEndAt(now.plus(trialDays, ChronoUnit.DAYS));
        } else {
            next.setStatus(status != null && !status.isBlank() ? status.trim().toLowerCase(Locale.ROOT) : "active");
            next.setTrialEndAt(null);
        }

        next = orgSubscriptionRepository.save(next);
        platformAuditService.logWithSnapshots(
                actorAuthId,
                "SUBSCRIPTION_UPSERTED",
                "Organisation",
                String.valueOf(organisationId),
                current != null ? subscriptionSnapshot(current) : null,
                subscriptionSnapshot(next),
                "plan=" + plan.getName() + ", cycle=" + cycle + ", status=" + next.getStatus() + ", trialDays=" + trialDays
        );
        return UpsertSubscriptionResult.success(next);
    }

    @Transactional(readOnly = true)
    public List<FeatureRolloutRule> listRollouts(Long organisationId) {
        return featureRolloutService.listOrganisationRules(organisationId);
    }

    @Transactional
    public void deleteRollout(Long organisationId, Long ruleId, Long actorAuthId) {
        featureRolloutService.deleteRule(organisationId, ruleId);
        platformAuditService.log(
                actorAuthId,
                "FEATURE_ROLLOUT_DELETED",
                "Organisation",
                String.valueOf(organisationId),
                "ruleId=" + ruleId
        );
    }

    @Transactional
    public UpsertRolloutResult upsertRollouts(Long organisationId, List<RolloutRuleInput> rules, Long actorAuthId) {
        if (rules == null || rules.isEmpty()) {
            return UpsertRolloutResult.error("rules is required");
        }

        List<FeatureRolloutRule> saved = new ArrayList<>();
        try {
            for (RolloutRuleInput ruleRequest : rules) {
                if (ruleRequest.scope() == null || ruleRequest.scope().isBlank()) {
                    return UpsertRolloutResult.invalidScopeResult();
                }
                FeatureRolloutRule.Scope scope = FeatureRolloutRule.Scope.valueOf(ruleRequest.scope().trim().toUpperCase(Locale.ROOT));
                if (CoreFeature.fromCode(ruleRequest.featureKey()) == null) {
                    throw new StoryApiException(HttpStatus.BAD_REQUEST, "UNKNOWN_FEATURE_KEY", "Unknown feature key: " + ruleRequest.featureKey());
                }
                FeatureRolloutRule savedRule = featureRolloutService.upsertRule(
                        organisationId,
                        scope,
                        ruleRequest.targetId(),
                        ruleRequest.targetKey(),
                        ruleRequest.featureKey(),
                        Boolean.TRUE.equals(ruleRequest.enabled()),
                        ruleRequest.usageLimit(),
                        ruleRequest.startAt(),
                        ruleRequest.endAt()
                );
                saved.add(savedRule);
            }
        } catch (IllegalArgumentException e) {
            return UpsertRolloutResult.invalidScopeResult();
        }

        platformAuditService.log(
                actorAuthId,
                "FEATURE_ROLLOUT_UPSERTED",
                "Organisation",
                String.valueOf(organisationId),
                "count=" + saved.size()
        );
        return UpsertRolloutResult.success(saved);
    }

    @Transactional(readOnly = true)
    public DunningPolicyView getEffectiveDunningPolicy() {
        var policy = subscriptionLifecycleService.getEffectivePolicy(null);
        return new DunningPolicyView(
                subscriptionLifecycleService.getPolicySteps(policy),
                policy.getGracePeriodDays(),
                policy.getTrialNoticeDaysBefore()
        );
    }

    @Transactional
    public DunningPolicyView upsertDunningPolicy(List<SubscriptionLifecycleService.DunningStep> steps,
                                                 Integer gracePeriodDays,
                                                 Integer trialNoticeDays,
                                                 Long actorAuthId) {
        var p = subscriptionLifecycleService.upsertPolicy(
                null,
                steps,
                gracePeriodDays,
                trialNoticeDays
        );
        var stepViews = subscriptionLifecycleService.getPolicySteps(p);
        platformAuditService.log(
                actorAuthId,
                "DUNNING_POLICY_UPDATED",
                "Organisation",
                "GLOBAL",
                "steps=" + stepViews.size() + ", graceDays=" + p.getGracePeriodDays() + ", trialNoticeDays=" + p.getTrialNoticeDaysBefore()
        );
        return new DunningPolicyView(stepViews, p.getGracePeriodDays(), p.getTrialNoticeDaysBefore());
    }

    @Transactional
    public Map<String, Integer> runBillingLifecycle(Long actorAuthId) {
        int notices = subscriptionLifecycleService.notifyTrialsExpiringSoon();
        int expired = subscriptionLifecycleService.expireTrialsAndMarkPastDue();
        int dunning = subscriptionLifecycleService.processPastDueDunning();
        platformAuditService.log(actorAuthId, "BILLING_LIFECYCLE_MANUAL_RUN", "System", "subscription-lifecycle",
                "notices=" + notices + ", expired=" + expired + ", dunning=" + dunning);
        return Map.of("notices", notices, "expired", expired, "dunning", dunning);
    }

    private static String defaultBillingCycle(SubscriptionPlan plan) {
        String cycle = plan.getBillingCycle() != null ? plan.getBillingCycle().toLowerCase(Locale.ROOT) : "";
        if (cycle.equals("monthly") || cycle.equals("yearly")) {
            return cycle;
        }
        return "monthly";
    }

    /** Legacy plan codes from older catalogs (Starter/Free/Pro renames). */
    private static final Map<String, String> LEGACY_PLAN_CODE_ALIASES = Map.of(
            "PROFESSIONAL", "PRO",
            "BASIC", "STARTER",
            "FREE", "STARTER",
            "PRO", "PROFESSIONAL"
    );

    /**
     * Resolve a subscription plan by catalog {@code code} or display {@code name}.
     * The strict subscription API accepts either (e.g. {@code PROFESSIONAL} or {@code Professional}).
     */
    private Optional<SubscriptionPlan> resolvePlanByCodeOrName(String planIdentifier) {
        if (planIdentifier == null || planIdentifier.isBlank()) {
            return Optional.empty();
        }
        String trimmed = planIdentifier.trim();
        Optional<SubscriptionPlan> byCode = subscriptionPlanRepository.findByCodeIgnoreCase(trimmed);
        if (byCode.isPresent()) {
            return byCode;
        }
        Optional<SubscriptionPlan> byName = subscriptionPlanRepository.findByNameIgnoreCase(trimmed);
        if (byName.isPresent()) {
            return byName;
        }
        String legacyCode = LEGACY_PLAN_CODE_ALIASES.get(trimmed.toUpperCase(Locale.ROOT));
        if (legacyCode != null) {
            return subscriptionPlanRepository.findByCodeIgnoreCase(legacyCode);
        }
        return Optional.empty();
    }

    private static String normalizeIncomingBillingCycle(String billingCycle, String fallbackInternal) {
        String incoming = billingCycle != null ? billingCycle.trim().toLowerCase(Locale.ROOT) : "";
        if ("monthly".equals(incoming)) {
            return "monthly";
        }
        if ("annual".equals(incoming) || "yearly".equals(incoming)) {
            return "yearly";
        }
        if (incoming.isBlank() && fallbackInternal != null && !fallbackInternal.isBlank()) {
            return fallbackInternal.toLowerCase(Locale.ROOT);
        }
        throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR",
                "billingCycle must be monthly, annual, or yearly");
    }

    private static BigDecimal resolvePrice(SubscriptionPlan plan, String cycle) {
        return SubscriptionPlanPriceResolver.resolvePlanPrice(plan, cycle);
    }

    private StrictSubscriptionView buildStrictView(OrgSubscription sub) {
        // Snapshot subscription/plan fields before tenant client counting.
        // TenantTransactionExecutor clears the EntityManager, which detaches this
        // entity and would otherwise LazyInitializationException on getPlan().
        Long orgId = sub.getOrganisation().getId();
        String schemaName = sub.getOrganisation().getSchemaName();
        Long subscriptionId = sub.getId();
        String planName = sub.getPlan() != null ? sub.getPlan().getName() : null;
        String status = sub.getStatus();
        String billingCycle = "yearly".equalsIgnoreCase(sub.getBillingCycleAtTime()) ? "annual" : "monthly";
        BigDecimal priceAtTime = sub.getPriceAtTime();
        Instant startAt = sub.getStartAt();
        Instant endAt = sub.getEndAt();
        Instant trialEndsAt = sub.getTrialEndAt();
        String providerCustomerId = sub.getProviderCustomerId();
        String providerSubscriptionId = sub.getProviderSubscriptionId();

        Integer therapistLimit = subscriptionFeatureService.getEffectiveLimit(orgId, CoreFeature.THERAPIST_LIMIT.getCode(), null);
        Integer supervisorLimit = subscriptionFeatureService.getEffectiveLimit(orgId, CoreFeature.SUPERVISOR_LIMIT.getCode(), null);
        Integer clientLimit = subscriptionFeatureService.getEffectiveLimit(orgId, CoreFeature.CLIENT_LIMIT.getCode(), null);
        long therapistUsers = authIdentityRoleRepository.countActiveByRoleForOrganisation(orgId, RoleName.THERAPIST.name());
        long supervisorUsers = authIdentityRoleRepository.countActiveByRoleForOrganisation(orgId, RoleName.SUPERVISOR.name());
        long clientUsers = countActiveClients(orgId, schemaName);

        return new StrictSubscriptionView(
                orgId,
                subscriptionId,
                planName,
                status,
                billingCycle,
                priceAtTime,
                startAt,
                endAt,
                trialEndsAt,
                providerCustomerId,
                providerSubscriptionId,
                therapistLimit,
                supervisorLimit,
                clientLimit,
                therapistUsers,
                supervisorUsers,
                clientUsers
        );
    }

    private long countActiveClients(Long organisationId, String schemaName) {
        if (!StringUtils.hasText(schemaName) || "public".equalsIgnoreCase(schemaName)) {
            return 0L;
        }
        try {
            Long count = tenantTransactionExecutor.executeReadOnly(
                    organisationId,
                    schemaName,
                    clientRepository::countNonDeleted);
            return count != null ? count : 0L;
        } catch (RuntimeException ex) {
            log.warn("Failed to count clients for organisation {} schema {}: {}",
                    organisationId, schemaName, ex.getMessage());
            return 0L;
        }
    }

    private void upsertUserLimitOverrides(Organisation organisation, UserLimitPatch patch) {
        if (patch == null) {
            return;
        }
        upsertSingleLimitOverride(organisation, CoreFeature.THERAPIST_LIMIT.getCode(), patch.therapistLimit());
        upsertSingleLimitOverride(organisation, CoreFeature.SUPERVISOR_LIMIT.getCode(), patch.supervisorLimit());
        upsertSingleLimitOverride(organisation, CoreFeature.CLIENT_LIMIT.getCode(), patch.clientLimit());
    }

    private void upsertSingleLimitOverride(Organisation organisation, String featureCode, Integer usageLimit) {
        if (usageLimit == null) {
            return;
        }
        if (usageLimit < 0) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", featureCode + " limit must be >= 0");
        }
        OrgFeatureOverride override = orgFeatureOverrideRepository
                .findByOrganisationIdAndFeatureKey(organisation.getId(), featureCode)
                .orElseGet(OrgFeatureOverride::new);
        override.setOrganisation(organisation);
        override.setFeatureKey(featureCode);
        override.setEnabled(true);
        override.setUsageLimit(usageLimit);
        orgFeatureOverrideRepository.save(override);
    }

    public record RolloutRuleInput(
            String scope,
            Long targetId,
            String targetKey,
            String featureKey,
            Boolean enabled,
            Integer usageLimit,
            Instant startAt,
            Instant endAt
    ) {}

    public record UpsertSubscriptionResult(boolean success, boolean notFound, String error, OrgSubscription subscription) {
        public static UpsertSubscriptionResult success(OrgSubscription subscription) {
            return new UpsertSubscriptionResult(true, false, null, subscription);
        }
        public static UpsertSubscriptionResult error(String error) {
            return new UpsertSubscriptionResult(false, false, error, null);
        }
        public static UpsertSubscriptionResult notFoundResult() {
            return new UpsertSubscriptionResult(false, true, null, null);
        }
    }

    public record UpsertRolloutResult(boolean success, String error, boolean invalidScope, List<FeatureRolloutRule> rules) {
        public static UpsertRolloutResult success(List<FeatureRolloutRule> rules) {
            return new UpsertRolloutResult(true, null, false, rules);
        }
        public static UpsertRolloutResult error(String error) {
            return new UpsertRolloutResult(false, error, false, List.of());
        }
        public static UpsertRolloutResult invalidScopeResult() {
            return new UpsertRolloutResult(false, "Invalid scope. Use ORGANISATION or THERAPIST", true, List.of());
        }
    }

    public record UserLimitPatch(Integer therapistLimit, Integer supervisorLimit, Integer clientLimit) {}

    public record DunningPolicyView(
            List<SubscriptionLifecycleService.DunningStep> steps,
            Integer gracePeriodDays,
            Integer trialNoticeDays
    ) {}

    public record StrictSubscriptionView(
            Long organisationId,
            Long subscriptionId,
            String plan,
            String status,
            String billingCycle,
            BigDecimal priceAtTime,
            Instant startAt,
            Instant endAt,
            Instant trialEndsAt,
            String providerCustomerId,
            String providerSubscriptionId,
            Integer therapistLimit,
            Integer supervisorLimit,
            Integer clientLimit,
            long therapistUsers,
            long supervisorUsers,
            long clientUsers
    ) {}

    public record SubscriptionStatusView(
            Long organisationId,
            Long subscriptionId,
            String plan,
            String status,
            String organisationStatus,
            Instant startAt,
            Instant endAt,
            Instant trialEndsAt,
            boolean isCurrent,
            boolean isTrialExpired,
            boolean isPastDue,
            boolean isCancelled,
            boolean isAccessRestricted,
            boolean providerCustomerConfigured,
            boolean providerSubscriptionConfigured,
            boolean providerBillingConfigured,
            String billingMode
    ) {}

    private OrgSubscription copySnapshot(OrgSubscription sub) {
        if (sub == null) {
            return null;
        }
        OrgSubscription copy = new OrgSubscription();
        copy.setId(sub.getId());
        copy.setOrganisation(sub.getOrganisation());
        copy.setPlan(sub.getPlan());
        copy.setStatus(sub.getStatus());
        copy.setStartAt(sub.getStartAt());
        copy.setEndAt(sub.getEndAt());
        copy.setTrialEndAt(sub.getTrialEndAt());
        copy.setPriceAtTime(sub.getPriceAtTime());
        copy.setBillingCycleAtTime(sub.getBillingCycleAtTime());
        copy.setProviderCustomerId(sub.getProviderCustomerId());
        copy.setProviderSubscriptionId(sub.getProviderSubscriptionId());
        copy.setDunningAttemptCount(sub.getDunningAttemptCount());
        copy.setNextDunningAt(sub.getNextDunningAt());
        copy.setLastPaymentFailedAt(sub.getLastPaymentFailedAt());
        copy.setLastDunningAt(sub.getLastDunningAt());
        copy.setNotifiedTrial(sub.getNotifiedTrial());
        copy.setCreatedAt(sub.getCreatedAt());
        return copy;
    }

    private Map<String, Object> subscriptionSnapshot(OrgSubscription sub) {
        if (sub == null) {
            return Map.of();
        }
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("id", sub.getId());
        snapshot.put("organisationId", sub.getOrganisation() != null ? sub.getOrganisation().getId() : null);
        snapshot.put("plan", sub.getPlan() != null ? sub.getPlan().getName() : null);
        snapshot.put("status", sub.getStatus());
        snapshot.put("billingCycle", sub.getBillingCycleAtTime());
        snapshot.put("priceAtTime", sub.getPriceAtTime());
        snapshot.put("startAt", sub.getStartAt());
        snapshot.put("endAt", sub.getEndAt());
        snapshot.put("trialEndAt", sub.getTrialEndAt());
        snapshot.put("providerCustomerId", sub.getProviderCustomerId());
        snapshot.put("providerSubscriptionId", sub.getProviderSubscriptionId());
        snapshot.put("dunningAttemptCount", sub.getDunningAttemptCount());
        snapshot.put("nextDunningAt", sub.getNextDunningAt());
        snapshot.put("lastPaymentFailedAt", sub.getLastPaymentFailedAt());
        snapshot.put("lastDunningAt", sub.getLastDunningAt());
        snapshot.put("notifiedTrial", sub.getNotifiedTrial());
        snapshot.put("createdAt", sub.getCreatedAt());
        return snapshot;
    }
}
