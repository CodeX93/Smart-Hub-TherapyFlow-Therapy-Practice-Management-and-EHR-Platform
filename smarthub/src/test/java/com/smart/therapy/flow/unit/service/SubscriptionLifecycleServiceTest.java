package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.payment.service.StripeDunningRetryService;
import com.smart.therapy.flow.subscription.entity.DunningPolicy;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.repository.DunningPolicyRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.BillingNotificationService;
import com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionLifecycleService tests")
class SubscriptionLifecycleServiceTest {

    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private OrganisationRepository organisationRepository;
    @Mock
    private DunningPolicyRepository dunningPolicyRepository;
    @Mock
    private PlatformAuditService platformAuditService;
    @Mock
    private BillingNotificationService billingNotificationService;
    @Mock
    private StripeDunningRetryService stripeDunningRetryService;
    @Mock
    private org.springframework.transaction.PlatformTransactionManager transactionManager;
    @Mock
    private org.springframework.beans.factory.ObjectProvider<io.micrometer.core.instrument.MeterRegistry> meterRegistryProvider;
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private SubscriptionLifecycleService subscriptionLifecycleService;

    @Test
    void skipsPaymentRetryBeforeGracePeriodHasElapsed() {
        var sub = baseSubscription("past_due");
        sub.setDunningAttemptCount(1);
        sub.setLastPaymentFailedAt(Instant.now());
        var policy = DunningPolicy.builder().gracePeriodDays(1)
                .stepsJson("[{\"day\":1,\"action\":\"email_reminder\"},{\"day\":2,\"action\":\"auto_suspend\"},{\"day\":3,\"action\":\"cancel\"}]")
                .build();
        when(orgSubscriptionRepository.findPastDueForDunningIds(eq("past_due"), any())).thenReturn(List.of(11L));
        when(orgSubscriptionRepository.findByIdForUpdateNowait(11L)).thenReturn(Optional.of(sub));
        when(dunningPolicyRepository.findByOrganisation_Id(99L)).thenReturn(Optional.of(policy));
        when(transactionManager.getTransaction(any())).thenReturn(new org.springframework.transaction.support.SimpleTransactionStatus());
        var result = subscriptionLifecycleService.processPastDueDunningBatch(10);
        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isZero();
        assertThat(sub.getDunningAttemptCount()).isEqualTo(1);
        org.mockito.Mockito.verifyNoInteractions(stripeDunningRetryService, billingNotificationService);
        org.mockito.Mockito.verify(orgSubscriptionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    void reportsLockContentionAndRollsBackWithoutRetryingPayment() {
        when(orgSubscriptionRepository.findPastDueForDunningIds(eq("past_due"), any())).thenReturn(List.of(11L));
        when(orgSubscriptionRepository.findByIdForUpdateNowait(11L)).thenThrow(
                new org.springframework.dao.CannotAcquireLockException("Synthetic competing worker"));
        var transaction = new org.springframework.transaction.support.SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(transaction);
        var result = subscriptionLifecycleService.processPastDueDunningBatch(10);
        assertThat(result.scannedCount()).isEqualTo(1);
        assertThat(result.processedCount()).isZero();
        assertThat(result.contentionCount()).isEqualTo(1);
        verify(transactionManager).rollback(transaction);
        org.mockito.Mockito.verifyNoInteractions(stripeDunningRetryService, billingNotificationService);
        org.mockito.Mockito.verify(orgSubscriptionRepository, org.mockito.Mockito.never()).save(any());
    }

    @Test
    @DisplayName("Rejects non-ascending steps with STEPS_NOT_ASCENDING")
    void shouldRejectNonAscendingSteps() {
        assertThatThrownBy(() -> subscriptionLifecycleService.upsertPolicy(
                null,
                List.of(
                        new SubscriptionLifecycleService.DunningStep(3, SubscriptionLifecycleService.DunningStep.Action.email_reminder),
                        new SubscriptionLifecycleService.DunningStep(2, SubscriptionLifecycleService.DunningStep.Action.auto_suspend),
                        new SubscriptionLifecycleService.DunningStep(7, SubscriptionLifecycleService.DunningStep.Action.cancel)
                ),
                2,
                2
        )).isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(e.getCode()).isEqualTo("STEPS_NOT_ASCENDING");
                });
    }

    @Test
    @DisplayName("Rejects cancel without preceding auto_suspend")
    void shouldRejectCancelWithoutSuspend() {
        assertThatThrownBy(() -> subscriptionLifecycleService.upsertPolicy(
                null,
                List.of(
                        new SubscriptionLifecycleService.DunningStep(1, SubscriptionLifecycleService.DunningStep.Action.email_reminder),
                        new SubscriptionLifecycleService.DunningStep(2, SubscriptionLifecycleService.DunningStep.Action.cancel)
                ),
                2,
                2
        )).isInstanceOf(StoryApiException.class)
                .satisfies(ex -> {
                    StoryApiException e = (StoryApiException) ex;
                    assertThat(e.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
                    assertThat(e.getCode()).isEqualTo("CANCEL_WITHOUT_SUSPEND");
                });
    }

    @Test
    @DisplayName("Trial notice marks notifiedTrial true and logs audit")
    void shouldDeduplicateTrialNotice() {
        DunningPolicy policy = DunningPolicy.builder()
                .trialNoticeDaysBefore(2)
                .stepsJson("[{\"day\":1,\"action\":\"email_reminder\"},{\"day\":2,\"action\":\"auto_suspend\"},{\"day\":3,\"action\":\"cancel\"}]")
                .gracePeriodDays(1)
                .build();
        OrgSubscription subscription = baseSubscription("trialing");
        subscription.setTrialEndAt(Instant.now().plus(1, ChronoUnit.DAYS));
        subscription.setNotifiedTrial(false);

        when(dunningPolicyRepository.findGlobalDefault()).thenReturn(Optional.of(policy));
        when(orgSubscriptionRepository.findTrialEndingBetweenNotifiedFalse(eq("trialing"), any(), any()))
                .thenReturn(List.of(subscription));

        int count = subscriptionLifecycleService.notifyTrialsExpiringSoon();

        assertThat(count).isEqualTo(1);
        assertThat(subscription.getNotifiedTrial()).isTrue();
        verify(orgSubscriptionRepository).save(subscription);
        verify(billingNotificationService).notifyTrialExpiryNotice(subscription, 2);
        verify(platformAuditService).log(eq(null), eq("TRIAL_EXPIRY_NOTICE"), eq("Organisation"), eq("99"), any());
    }

    @Test
    @DisplayName("Expired trial moves to past_due with retry_count initialized to 1")
    void shouldExpireTrialToPastDue() {
        DunningPolicy policy = DunningPolicy.builder()
                .trialNoticeDaysBefore(2)
                .gracePeriodDays(2)
                .stepsJson("[{\"day\":1,\"action\":\"email_reminder\"},{\"day\":3,\"action\":\"auto_suspend\"},{\"day\":7,\"action\":\"cancel\"}]")
                .build();
        OrgSubscription sub = baseSubscription("trialing");
        sub.setTrialEndAt(Instant.now().minus(1, ChronoUnit.DAYS));

        when(orgSubscriptionRepository.findTrialingExpired(eq("trialing"), any())).thenReturn(List.of(sub));
        when(dunningPolicyRepository.findByOrganisation_Id(99L)).thenReturn(Optional.of(policy));

        int count = subscriptionLifecycleService.expireTrialsAndMarkPastDue();

        assertThat(count).isEqualTo(1);
        assertThat(sub.getStatus()).isEqualTo("past_due");
        assertThat(sub.getDunningAttemptCount()).isEqualTo(1);
        assertThat(sub.getNextDunningAt()).isNotNull();
        verify(orgSubscriptionRepository).save(sub);
    }

    @Test
    @DisplayName("Dunning processing fires action and increments retry count")
    void shouldProcessDunningStep() {
        DunningPolicy policy = DunningPolicy.builder()
                .gracePeriodDays(1)
                .stepsJson("[{\"day\":1,\"action\":\"email_reminder\"},{\"day\":2,\"action\":\"auto_suspend\"},{\"day\":3,\"action\":\"cancel\"}]")
                .autoLockOnCancel(true)
                .build();
        OrgSubscription sub = baseSubscription("past_due");
        sub.setDunningAttemptCount(1);
        sub.setLastPaymentFailedAt(Instant.now().minus(3, ChronoUnit.DAYS));

        when(orgSubscriptionRepository.findPastDueForDunningIds(eq("past_due"), any())).thenReturn(List.of(sub.getId()));
        when(orgSubscriptionRepository.findByIdForUpdateNowait(sub.getId())).thenReturn(Optional.of(sub));
        var transaction = new org.springframework.transaction.support.SimpleTransactionStatus();
        when(transactionManager.getTransaction(any())).thenReturn(transaction);
        when(dunningPolicyRepository.findByOrganisation_Id(99L)).thenReturn(Optional.of(policy));
        when(stripeDunningRetryService.retryOpenInvoicePayment(sub)).thenReturn(false);

        int processed = subscriptionLifecycleService.processPastDueDunning();

        assertThat(processed).isEqualTo(1);
        assertThat(sub.getDunningAttemptCount()).isEqualTo(2);
        verify(billingNotificationService).notifyDunningAttempt(sub, 1);
        assertThat(sub.getTransactionExecutionId()).isNotBlank();
        assertThat(sub.getNextDunningAt()).isEqualTo(sub.getLastPaymentFailedAt().plus(3, ChronoUnit.DAYS));
        verify(transactionManager).getTransaction(org.mockito.ArgumentMatchers.argThat(definition ->
                definition.getPropagationBehavior() == org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW));
        verify(transactionManager).commit(transaction);
        verify(orgSubscriptionRepository).save(sub);
    }

    private static OrgSubscription baseSubscription(String status) {
        Organisation organisation = new Organisation();
        organisation.setId(99L);
        organisation.setStatus("ACTIVE");

        OrgSubscription sub = new OrgSubscription();
        sub.setId(11L);
        sub.setOrganisation(organisation);
        sub.setStatus(status);
        sub.setStartAt(Instant.now().minus(30, ChronoUnit.DAYS));
        sub.setNotifiedTrial(false);
        return sub;
    }
}
