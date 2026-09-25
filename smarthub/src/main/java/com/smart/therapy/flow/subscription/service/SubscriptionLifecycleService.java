package com.smart.therapy.flow.subscription.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.payment.service.StripeDunningRetryService;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.subscription.entity.DunningPolicy;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.repository.DunningPolicyRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Enterprise lifecycle state machine for tenant subscription status:
 * trialing -> past_due -> active (on payment success) OR cancelled (after dunning retries exhausted).
 * Restriction is enforced by changing organisation status to LOCKED on cancellation due to non-payment.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionLifecycleService {

    private static final String STATUS_TRIALING = "trialing";
    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_PAST_DUE = "past_due";
    private static final String STATUS_CANCELLED = "cancelled";

    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final OrganisationRepository organisationRepository;
    private final DunningPolicyRepository dunningPolicyRepository;
    private final PlatformAuditService platformAuditService;
    private final BillingNotificationService billingNotificationService;
    private final StripeDunningRetryService stripeDunningRetryService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthSessionService authSessionService;
    private final TenantDirectoryService tenantDirectoryService;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    @Transactional
    public int notifyTrialsExpiringSoon() {
        Instant now = Instant.now();
        DunningPolicy globalPolicy = getGlobalPolicy();
        int noticeDays = Math.max(1, globalPolicy.getTrialNoticeDaysBefore() != null ? globalPolicy.getTrialNoticeDaysBefore() : 1);
        Instant endWindow = now.plus(noticeDays, ChronoUnit.DAYS);

        List<OrgSubscription> soonExpiring = orgSubscriptionRepository.findTrialEndingBetweenNotifiedFalse(STATUS_TRIALING, now, endWindow);
        for (OrgSubscription sub : soonExpiring) {
            DunningPolicy policy = getPolicyForOrg(sub.getOrganisation().getId());
            int orgNoticeDays = Math.max(1, policy.getTrialNoticeDaysBefore() != null ? policy.getTrialNoticeDaysBefore() : noticeDays);
            billingNotificationService.notifyTrialExpiryNotice(sub, orgNoticeDays);
            sub.setNotifiedTrial(true);
            orgSubscriptionRepository.save(sub);
            platformAuditService.log(
                    null,
                    "TRIAL_EXPIRY_NOTICE",
                    "Organisation",
                    String.valueOf(sub.getOrganisation().getId()),
                    "trialEndAt=" + sub.getTrialEndAt() + ", daysBefore=" + orgNoticeDays
            );
        }
        return soonExpiring.size();
    }

    @Transactional
    public int expireTrialsAndMarkPastDue() {
        Instant now = Instant.now();
        List<OrgSubscription> expired = orgSubscriptionRepository.findTrialingExpired(STATUS_TRIALING, now);
        int processed = 0;
        for (OrgSubscription sub : expired) {
            if (SubscriptionPlanPriceResolver.isProviderManaged(
                    sub.getPlan(), sub.getProviderCustomerId(), sub.getProviderSubscriptionId())) {
                continue;
            }
            DunningPolicy policy = getPolicyForOrg(sub.getOrganisation().getId());
            int firstStepDay = resolveSteps(policy).get(0).day();
            sub.setStatus(STATUS_PAST_DUE);
            sub.setLastPaymentFailedAt(now);
            sub.setDunningAttemptCount(1);
            sub.setNextDunningAt(now.plus(policy.getGracePeriodDays() + firstStepDay, ChronoUnit.DAYS));
            orgSubscriptionRepository.save(sub);
            billingNotificationService.notifyTrialExpiredPastDue(sub);

            platformAuditService.log(
                    null,
                    "TRIAL_EXPIRED_PAST_DUE",
                    "Organisation",
                    String.valueOf(sub.getOrganisation().getId()),
                    "subscriptionId=" + sub.getId()
            );
            processed++;
        }
        return processed;
    }

    @Transactional
    public int notifySubscriptionsEndingSoon(int daysBefore) {
        int boundedDaysBefore = Math.max(1, daysBefore);
        Instant now = Instant.now();
        Instant noticeWindowEnd = now.plus(boundedDaysBefore, ChronoUnit.DAYS);
        int notices = 0;

        List<OrgSubscription> currentSubs = orgSubscriptionRepository.findAllCurrentWithOrganisationAndPlan();
        for (OrgSubscription sub : currentSubs) {
            Instant termEndAt = resolvePlanTermEndAt(sub);
            if (termEndAt == null || !termEndAt.isAfter(now) || termEndAt.isAfter(noticeWindowEnd)) {
                continue;
            }
            boolean recentlySent = billingNotificationService.wasEventSentRecently(
                    sub.getOrganisation().getId(),
                    BillingNotificationService.EVENT_SUBSCRIPTION_TERM_ENDING_SOON,
                    java.time.Duration.ofHours(36)
            );
            if (recentlySent) {
                continue;
            }
            billingNotificationService.notifySubscriptionTermEndingSoon(sub, boundedDaysBefore, termEndAt);
            platformAuditService.log(
                    null,
                    "SUBSCRIPTION_ENDING_SOON_NOTICE",
                    "Organisation",
                    String.valueOf(sub.getOrganisation().getId()),
                    "subscriptionId=" + sub.getId() + ", endsAt=" + termEndAt + ", daysBefore=" + boundedDaysBefore
            );
            notices++;
        }
        return notices;
    }

    @Transactional
    public int enforceEndedSubscriptions() {
        Instant now = Instant.now();
        int ended = 0;
        List<OrgSubscription> currentSubs = orgSubscriptionRepository.findAllCurrentWithOrganisationAndPlan();
        for (OrgSubscription sub : currentSubs) {
            if (STATUS_CANCELLED.equalsIgnoreCase(sub.getStatus())) {
                continue;
            }
            Instant termEndAt = resolvePlanTermEndAt(sub);
            if (termEndAt == null || termEndAt.isAfter(now)) {
                continue;
            }

            sub.setStatus(STATUS_CANCELLED);
            sub.setEndAt(termEndAt);
            sub.setNextDunningAt(null);
            sub.setTransactionExecutionId(newTxExecutionId());
            orgSubscriptionRepository.save(sub);

            lockOrganisation(sub.getOrganisation(), "Subscription term ended at " + termEndAt);
            boolean recentlySent = billingNotificationService.wasEventSentRecently(
                    sub.getOrganisation().getId(),
                    BillingNotificationService.EVENT_SUBSCRIPTION_TERM_ENDED,
                    java.time.Duration.ofHours(36)
            );
            if (!recentlySent) {
                billingNotificationService.notifySubscriptionTermEnded(sub, termEndAt);
            }
            platformAuditService.log(
                    null,
                    "SUBSCRIPTION_ENDED_BY_TERM",
                    "Organisation",
                    String.valueOf(sub.getOrganisation().getId()),
                    "subscriptionId=" + sub.getId() + ", termEndAt=" + termEndAt
            );
            ended++;
        }
        return ended;
    }

    public int processPastDueDunning() {
        int totalProcessed = 0;
        int safety = 0;
        while (safety++ < 20) {
            DunningBatchResult batch = processPastDueDunningBatch(200);
            totalProcessed += batch.processedCount();
            if (batch.scannedCount() < 200) {
                break;
            }
            if (batch.processedCount() == 0 && batch.contentionCount() == 0) {
                break;
            }
        }
        return totalProcessed;
    }

    public DunningBatchResult processPastDueDunningBatch(int maxItems) {
        int bounded = Math.max(1, Math.min(maxItems, 500));
        List<Long> ids = orgSubscriptionRepository.findPastDueForDunningIds(
                STATUS_PAST_DUE,
                org.springframework.data.domain.PageRequest.of(0, bounded)
        );
        int processed = 0;
        int contention = 0;
        for (Long id : ids) {
            String txExecutionId = newTxExecutionId();
            try {
                boolean changed = executeRequiresNew(() -> processPastDueDunningOneLocked(id, txExecutionId));
                if (changed) {
                    processed++;
                }
            } catch (PessimisticLockingFailureException ex) {
                contention++;
                incrementCounter("subscription.lifecycle.lock.contention");
                log.debug("Skipping dunning processing due to lock contention subscriptionId={}, txExecutionId={}", id, txExecutionId);
            }
        }
        return new DunningBatchResult(ids.size(), processed, contention);
    }

    private boolean processPastDueDunningOneLocked(Long subscriptionId, String txExecutionId) {
        Instant now = Instant.now();
        OrgSubscription sub = orgSubscriptionRepository.findByIdForUpdateNowait(subscriptionId).orElse(null);
        if (sub == null || !STATUS_PAST_DUE.equalsIgnoreCase(sub.getStatus()) || !isCurrentAt(sub, now)) {
            return false;
        }
        DunningPolicy policy = getPolicyForOrg(sub.getOrganisation().getId());
        List<DunningStep> steps = resolveSteps(policy);
        int attempt = Math.max(1, Optional.ofNullable(sub.getDunningAttemptCount()).orElse(1));

        if (attempt > steps.size()) {
            sub.setStatus(STATUS_CANCELLED);
            sub.setEndAt(now);
            sub.setNextDunningAt(null);
            if (Boolean.TRUE.equals(policy.getAutoLockOnCancel())) {
                lockOrganisation(sub.getOrganisation(), "Subscription cancelled after dunning retries");
            }
            billingNotificationService.notifyCancelledForNonPayment(sub);
            sub.setLastDunningAt(now);
            sub.setTransactionExecutionId(txExecutionId);
            orgSubscriptionRepository.save(sub);
            return true;
        }

        if (sub.getLastPaymentFailedAt() == null) {
            return false;
        }
        long daysOverdue = ChronoUnit.DAYS.between(sub.getLastPaymentFailedAt(), now);
        DunningStep currentStep = steps.get(attempt - 1);
        int threshold = policy.getGracePeriodDays() + currentStep.day();
        if (daysOverdue < threshold) {
            return false;
        }

        boolean paymentRetryTriggered = stripeDunningRetryService.retryOpenInvoicePayment(sub);
        applyDunningAction(sub, currentStep.action(), attempt, paymentRetryTriggered, now);

        int nextAttempt = attempt + 1;
        sub.setDunningAttemptCount(nextAttempt);
        sub.setLastDunningAt(now);
        if (nextAttempt > steps.size() && !STATUS_CANCELLED.equalsIgnoreCase(sub.getStatus())) {
            sub.setStatus(STATUS_CANCELLED);
            sub.setEndAt(now);
            sub.setNextDunningAt(null);
            if (Boolean.TRUE.equals(policy.getAutoLockOnCancel())) {
                lockOrganisation(sub.getOrganisation(), "Subscription cancelled after dunning retries");
            }
            billingNotificationService.notifyCancelledForNonPayment(sub);
        } else if (nextAttempt <= steps.size()) {
            int nextThresholdDays = policy.getGracePeriodDays() + steps.get(nextAttempt - 1).day();
            sub.setNextDunningAt(sub.getLastPaymentFailedAt().plus(nextThresholdDays, ChronoUnit.DAYS));
        } else {
            sub.setNextDunningAt(null);
        }
        sub.setTransactionExecutionId(txExecutionId);
        orgSubscriptionRepository.save(sub);
        return true;
    }

    @Transactional
    public void onPaymentFailed(Long organisationId, String reason) {
        String txExecutionId = newTxExecutionId();
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationIdForUpdateNowait(organisationId).orElse(null);
        if (sub == null) {
            return;
        }
        Instant now = Instant.now();
        DunningPolicy policy = getPolicyForOrg(organisationId);
        int firstStepDay = resolveSteps(policy).get(0).day();
        sub.setStatus(STATUS_PAST_DUE);
        sub.setLastPaymentFailedAt(now);
        sub.setDunningAttemptCount(1);
        sub.setNextDunningAt(now.plus(policy.getGracePeriodDays() + firstStepDay, ChronoUnit.DAYS));
        sub.setTransactionExecutionId(txExecutionId);
        orgSubscriptionRepository.save(sub);
        billingNotificationService.notifyDunningAttempt(sub, 1);
        platformAuditService.log(null, "PAYMENT_FAILED_PAST_DUE", "Organisation", String.valueOf(organisationId), reason + ", txExecutionId=" + txExecutionId);
    }

    @Transactional
    public void onPaymentSucceeded(Long organisationId, String details) {
        String txExecutionId = newTxExecutionId();
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationIdForUpdateNowait(organisationId).orElse(null);
        if (sub == null) {
            return;
        }
        Instant now = Instant.now();
        Instant termEndAt = resolvePlanTermEndAt(sub);
        if (termEndAt != null && !termEndAt.isAfter(now)) {
            sub.setStatus(STATUS_CANCELLED);
            sub.setEndAt(termEndAt);
            sub.setNextDunningAt(null);
            sub.setTransactionExecutionId(txExecutionId);
            orgSubscriptionRepository.save(sub);
            lockOrganisation(sub.getOrganisation(), "Payment succeeded but subscription term already ended");
            platformAuditService.log(
                    null,
                    "PAYMENT_SUCCEEDED_IGNORED_TERM_ENDED",
                    "Organisation",
                    String.valueOf(organisationId),
                    details + ", termEndAt=" + termEndAt + ", txExecutionId=" + txExecutionId
            );
            return;
        }
        sub.setStatus(STATUS_ACTIVE);
        sub.setDunningAttemptCount(1);
        sub.setNextDunningAt(null);
        sub.setLastPaymentFailedAt(null);
        sub.setLastDunningAt(null);
        if (!SubscriptionPlanPriceResolver.isProviderManaged(
                sub.getPlan(), sub.getProviderCustomerId(), sub.getProviderSubscriptionId())) {
            sub.setStartAt(now);
            sub.setEndAt(null);
        } else if (sub.getProviderCurrentPeriodEnd() != null && sub.getProviderCurrentPeriodEnd().isAfter(now)) {
            sub.setEndAt(null);
        }
        sub.setTransactionExecutionId(txExecutionId);
        orgSubscriptionRepository.save(sub);
        billingNotificationService.notifyPaymentRecovered(sub);

        unlockOrganisationIfLocked(sub.getOrganisation());
        platformAuditService.log(null, "PAYMENT_SUCCEEDED_ACTIVE", "Organisation", String.valueOf(organisationId), details + ", txExecutionId=" + txExecutionId);
    }

    @Transactional
    public void onProviderSubscriptionCancelled(Long organisationId, String details) {
        String txExecutionId = newTxExecutionId();
        OrgSubscription sub = orgSubscriptionRepository.findCurrentByOrganisationIdForUpdateNowait(organisationId).orElse(null);
        if (sub == null) {
            return;
        }
        sub.setStatus(STATUS_CANCELLED);
        sub.setEndAt(Instant.now());
        sub.setNextDunningAt(null);
        sub.setTransactionExecutionId(txExecutionId);
        orgSubscriptionRepository.save(sub);
        billingNotificationService.notifyCancelledForNonPayment(sub);
        lockOrganisation(sub.getOrganisation(), "Provider subscription cancelled");
        platformAuditService.log(null, "SUBSCRIPTION_CANCELLED", "Organisation", String.valueOf(organisationId), details + ", txExecutionId=" + txExecutionId);
    }

    @Transactional(readOnly = true)
    public DunningPolicy getEffectivePolicy(Long organisationId) {
        return getPolicyForOrg(organisationId);
    }

    @Transactional
    public DunningPolicy upsertPolicy(Long organisationId, List<DunningStep> steps, Integer gracePeriodDays, Integer trialNoticeDays) {
        DunningPolicy p = organisationId == null
                ? dunningPolicyRepository.findGlobalDefault().orElseGet(DunningPolicy::new)
                : dunningPolicyRepository.findByOrganisation_Id(organisationId).orElseGet(DunningPolicy::new);

        if (organisationId != null && p.getOrganisation() == null) {
            Organisation org = organisationRepository.findById(organisationId).orElseThrow();
            p.setOrganisation(org);
        }
        List<DunningStep> validated = validateSteps(steps);
        p.setStepsJson(writeStepsJson(validated));
        p.setGracePeriodDays(gracePeriodDays);
        p.setTrialNoticeDaysBefore(trialNoticeDays);

        // Backward compatible columns are still persisted for legacy analytics/queries.
        p.setAutoLockOnCancel(true);
        p.setRetry1Hours(validated.size() > 0 ? validated.get(0).day() * 24 : 24);
        p.setRetry2Hours(validated.size() > 1 ? validated.get(1).day() * 24 : 72);
        p.setRetry3Hours(validated.size() > 2 ? validated.get(2).day() * 24 : 120);

        Instant now = Instant.now();
        if (p.getCreatedAt() == null) p.setCreatedAt(now);
        p.setUpdatedAt(now);
        return dunningPolicyRepository.save(p);
    }

    @Transactional(readOnly = true)
    public List<DunningStep> getPolicySteps(DunningPolicy policy) {
        return resolveSteps(policy);
    }

    private DunningPolicy getPolicyForOrg(Long organisationId) {
        if (organisationId != null) {
            Optional<DunningPolicy> orgPolicy = dunningPolicyRepository.findByOrganisation_Id(organisationId);
            if (orgPolicy.isPresent()) {
                return orgPolicy.get();
            }
        }
        return getGlobalPolicy();
    }

    private DunningPolicy getGlobalPolicy() {
        return dunningPolicyRepository.findGlobalDefault().orElseGet(() -> DunningPolicy.builder()
                .stepsJson(writeStepsJson(defaultSteps()))
                .retry1Hours(24)
                .retry2Hours(72)
                .retry3Hours(120)
                .gracePeriodDays(3)
                .trialNoticeDaysBefore(7)
                .autoLockOnCancel(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build());
    }

    private void lockOrganisation(Organisation org, String reason) {
        if (org == null) return;
        org.setStatus("LOCKED");
        organisationRepository.save(org);
        revokeOrganisationSessions(org.getId());
        tenantDirectoryService.evictCache();
        platformAuditService.log(null, "ORG_LOCKED_FOR_NON_PAYMENT", "Organisation", String.valueOf(org.getId()), reason);
    }

    private void unlockOrganisationIfLocked(Organisation org) {
        if (org == null) return;
        if ("LOCKED".equalsIgnoreCase(org.getStatus())) {
            org.setStatus("ACTIVE");
            organisationRepository.save(org);
            tenantDirectoryService.evictCache();
            platformAuditService.log(null, "ORG_UNLOCKED_AFTER_PAYMENT", "Organisation", String.valueOf(org.getId()), "Payment recovered");
        }
    }

    private void revokeOrganisationSessions(Long organisationId) {
        if (organisationId == null) {
            return;
        }
        authIdentityRepository.findByOrganisation_Id(organisationId).forEach(identity -> {
            if (identity != null && identity.getId() != null) {
                authSessionService.revokeAllForAuthId(identity.getId());
            }
        });
    }

    private void applyDunningAction(OrgSubscription sub,
                                    DunningStep.Action action,
                                    int attempt,
                                    boolean paymentRetryTriggered,
                                    Instant now) {
        String details = "subscriptionId=" + sub.getId()
                + ", attempt=" + attempt
                + ", action=" + action.name()
                + ", paymentRetryTriggered=" + paymentRetryTriggered;

        if (action == DunningStep.Action.email_reminder) {
            billingNotificationService.notifyDunningAttempt(sub, attempt);
        } else if (action == DunningStep.Action.auto_suspend) {
            lockOrganisation(sub.getOrganisation(), "Auto suspend from dunning policy");
            billingNotificationService.notifyDunningAttempt(sub, attempt);
        } else if (action == DunningStep.Action.cancel) {
            sub.setStatus(STATUS_CANCELLED);
            sub.setEndAt(now);
            sub.setNextDunningAt(null);
            if (Boolean.TRUE.equals(getPolicyForOrg(sub.getOrganisation().getId()).getAutoLockOnCancel())) {
                lockOrganisation(sub.getOrganisation(), "Cancellation from dunning policy");
            }
            billingNotificationService.notifyCancelledForNonPayment(sub);
        }

        platformAuditService.log(
                null,
                "DUNNING_ATTEMPT",
                "Organisation",
                String.valueOf(sub.getOrganisation().getId()),
                details + ", status=" + sub.getStatus()
        );
    }

    private List<DunningStep> resolveSteps(DunningPolicy policy) {
        if (policy == null) {
            return defaultSteps();
        }
        String raw = policy.getStepsJson();
        if (raw == null || raw.isBlank()) {
            return fallbackStepsFromLegacyColumns(policy);
        }
        try {
            List<DunningStep> parsed = objectMapper.readValue(raw, new TypeReference<>() {});
            if (parsed == null || parsed.isEmpty()) {
                return fallbackStepsFromLegacyColumns(policy);
            }
            return parsed;
        } catch (Exception ex) {
            log.warn("Failed to parse dunning policy steps_json policyId={}: {}", policy.getId(), ex.getMessage());
            return fallbackStepsFromLegacyColumns(policy);
        }
    }

    private List<DunningStep> fallbackStepsFromLegacyColumns(DunningPolicy policy) {
        List<DunningStep> steps = new ArrayList<>();
        steps.add(new DunningStep(Math.max(1, safeDayFromHours(policy.getRetry1Hours())), DunningStep.Action.email_reminder));
        steps.add(new DunningStep(Math.max(2, safeDayFromHours(policy.getRetry2Hours())), DunningStep.Action.auto_suspend));
        steps.add(new DunningStep(Math.max(3, safeDayFromHours(policy.getRetry3Hours())), DunningStep.Action.cancel));
        return steps;
    }

    private int safeDayFromHours(Integer hours) {
        int value = hours == null ? 24 : hours;
        return Math.max(1, (int) Math.ceil(value / 24.0));
    }

    private List<DunningStep> validateSteps(List<DunningStep> steps) {
        if (steps == null || steps.isEmpty()) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "steps are required");
        }
        for (int i = 1; i < steps.size(); i++) {
            if (steps.get(i).day() <= steps.get(i - 1).day()) {
                throw new StoryApiException(HttpStatus.BAD_REQUEST, "STEPS_NOT_ASCENDING", "step.day values must be strictly ascending");
            }
        }

        boolean seenAutoSuspend = false;
        boolean seenEmail = false;
        boolean hasCancel = false;
        for (DunningStep step : steps) {
            if (step.action() == DunningStep.Action.email_reminder) {
                seenEmail = true;
            }
            if (step.action() == DunningStep.Action.auto_suspend) {
                seenAutoSuspend = true;
                if (!seenEmail) {
                    throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "NO_EMAIL_BEFORE_SUSPEND",
                            "email_reminder must precede auto_suspend");
                }
            }
            if (step.action() == DunningStep.Action.cancel) {
                hasCancel = true;
                if (!seenAutoSuspend) {
                    throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "CANCEL_WITHOUT_SUSPEND",
                            "cancel step must be preceded by auto_suspend");
                }
            }
        }
        if (!hasCancel) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "VALIDATION_ERROR", "cancel step is required");
        }
        return steps;
    }

    private String writeStepsJson(List<DunningStep> steps) {
        try {
            return objectMapper.writeValueAsString(steps);
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Failed to serialize dunning steps");
        }
    }

    private List<DunningStep> defaultSteps() {
        return List.of(
                new DunningStep(1, DunningStep.Action.email_reminder),
                new DunningStep(3, DunningStep.Action.auto_suspend),
                new DunningStep(7, DunningStep.Action.cancel)
        );
    }

    private boolean executeRequiresNew(java.util.function.Supplier<Boolean> action) {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Boolean result = template.execute(status -> action.get());
        return Boolean.TRUE.equals(result);
    }

    private static boolean isCurrentAt(OrgSubscription sub, Instant at) {
        if (sub == null || sub.getStartAt() == null || sub.getStartAt().isAfter(at)) {
            return false;
        }
        return sub.getEndAt() == null || sub.getEndAt().isAfter(at);
    }

    private static Instant resolvePlanTermEndAt(OrgSubscription sub) {
        if (sub == null || sub.getStartAt() == null) {
            return null;
        }
        if (sub.getEndAt() != null) {
            return sub.getEndAt();
        }
        if (SubscriptionPlanPriceResolver.isProviderManaged(
                sub.getPlan(), sub.getProviderCustomerId(), sub.getProviderSubscriptionId())
                && sub.getProviderCurrentPeriodEnd() != null) {
            return sub.getProviderCurrentPeriodEnd();
        }
        if (STATUS_TRIALING.equalsIgnoreCase(sub.getStatus()) && sub.getTrialEndAt() != null) {
            return sub.getTrialEndAt();
        }

        String cycle = sub.getBillingCycleAtTime() != null ? sub.getBillingCycleAtTime().trim().toLowerCase() : "";
        ZonedDateTime start = sub.getStartAt().atZone(ZoneOffset.UTC);
        if ("yearly".equals(cycle) || "annual".equals(cycle)) {
            return start.plusYears(1).toInstant();
        }
        if ("monthly".equals(cycle)) {
            return start.plusMonths(1).toInstant();
        }
        if ("daily".equals(cycle)) {
            return start.plusDays(1).toInstant();
        }
        if (cycle.endsWith("_days")) {
            String dayPart = cycle.substring(0, cycle.length() - "_days".length());
            try {
                int days = Integer.parseInt(dayPart);
                if (days > 0) {
                    return start.plusDays(days).toInstant();
                }
            } catch (NumberFormatException ignored) {
                // fall through to null
            }
        }
        return null;
    }

    private static String newTxExecutionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    private void incrementCounter(String metric) {
        MeterRegistry registry = meterRegistryProvider.getIfAvailable();
        if (registry == null) {
            return;
        }
        Counter.builder(metric).register(registry).increment();
    }

    public record DunningBatchResult(int scannedCount, int processedCount, int contentionCount) {
    }

    public record DunningStep(Integer day, Action action) {
        public enum Action {
            email_reminder,
            auto_suspend,
            cancel
        }
    }
}
