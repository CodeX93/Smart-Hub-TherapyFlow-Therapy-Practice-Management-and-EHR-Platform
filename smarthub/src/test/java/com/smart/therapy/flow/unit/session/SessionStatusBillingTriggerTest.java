package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.dto.SessionBillingResponse;
import com.smart.therapy.flow.billing.entity.SessionBilling;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientMrnService;
import com.smart.therapy.flow.client.service.ClientSearchHelper;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionIntegrationRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.session.service.SessionOutcomeTransactionService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Session status auto-billing + outcome timing guards")
class SessionStatusBillingTriggerTest {

    private static final ZoneId PRACTICE_ZONE = ZoneId.of("America/New_York");

    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientMrnService clientMrnService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private RoomRepository roomRepository;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private TimezoneService timezoneService;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private CaseloadScopeService caseloadScopeService;
    @Mock
    private SubscriptionFeatureService subscriptionFeatureService;
    @Mock
    private SystemOptionResolverService systemOptionResolverService;
    @Mock
    private ClientSearchHelper clientSearchHelper;
    @Mock
    private SessionTranscriptRepository sessionTranscriptRepository;
    @Mock
    private SessionIntegrationRepository sessionIntegrationRepository;
    @Mock
    private TherapistAvailabilityService therapistAvailabilityService;
    @Mock
    private BillingService billingService;
    @Mock
    private SessionOutcomeTransactionService sessionOutcomeTransactionService;

    @InjectMocks
    private SessionService sessionService;

    @Mock
    private jakarta.persistence.EntityManager entityManager;

    private AuthPrincipal principal;
    private User therapist;
    private Client client;
    private Session session;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(sessionService, "billingService", billingService);
        ReflectionTestUtils.setField(sessionService, "entityManager", entityManager);

        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        principal = TestDataFactory.createAuthPrincipal(therapist);
        client = TestDataFactory.createTestClient();
        client.setId(10L);
        client.setClientId("CL-10");
        client.setAssignedTherapist(therapist);

        session = TestDataFactory.createTestSession(client, therapist);
        session.setId(100L);
        session.setStatus(SessionStatus.SCHEDULED.getValue());
        // Past session day so completed/no-show billing tests are not blocked by timing guard
        session.setSessionDate(Instant.now().minus(2, ChronoUnit.DAYS));

        lenient().when(currentUserService.requireCurrentUser(principal)).thenReturn(therapist);
        lenient().when(caseloadScopeService.resolve(principal))
                .thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(CaseloadScope.ALL, List.of(), 1L));
        lenient().when(sessionRepository.findByIdWithRelations(100L)).thenReturn(Optional.of(session));
        lenient().when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(systemOptionResolverService.requireOptionKey(eq(SystemOptionCategories.SESSION_STATUS), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(clientRepository.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(sessionRepository.findLastHeldSessionDate(anyLong(), any())).thenReturn(null);
        lenient().when(sessionRepository.findNextAppointmentDate(anyLong(), any())).thenReturn(null);
        lenient().when(timezoneService.findPracticeTimezoneId()).thenReturn(Optional.of(PRACTICE_ZONE.getId()));
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "no-show", "no_show"})
    @DisplayName("shouldCreateBillingForStatus is true for completed and no-show")
    void shouldCreateBillingForBillableStatuses(String status) {
        Boolean result = ReflectionTestUtils.invokeMethod(sessionService, "shouldCreateBillingForStatus", status);
        assertThat(result).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"cancelled", "rescheduling", "rescheduled", "scheduled", "confirmed"})
    @DisplayName("shouldCreateBillingForStatus is false for cancelled/reschedule/active statuses")
    void shouldNotCreateBillingForNonBillableStatuses(String status) {
        Boolean result = ReflectionTestUtils.invokeMethod(sessionService, "shouldCreateBillingForStatus", status);
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("Completed status creates billing")
    void completedCreatesBilling() {
        SessionBillingResponse billing = SessionBillingResponse.builder()
                        .id(55L)
                        .totalAmount(new BigDecimal("150.00"))
                        .amountDue(new BigDecimal("150.00"))
                        .build();
        when(sessionOutcomeTransactionService.applyOutcomeAndCreateBilling(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1"))
                .thenReturn(new SessionOutcomeTransactionService.OutcomeResult(session, billing));

        sessionService.updateSessionStatus(100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");

        verify(sessionOutcomeTransactionService).persistScheduledFallback(100L);
        verify(sessionOutcomeTransactionService).applyOutcomeAndCreateBilling(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");
    }

    @Test
    @DisplayName("No-show status creates billing")
    void noShowCreatesBilling() {
        SessionBillingResponse billing = SessionBillingResponse.builder().id(56L).build();
        when(sessionOutcomeTransactionService.applyOutcomeAndCreateBilling(
                100L, SessionStatus.NO_SHOW.getValue(), principal, "127.0.0.1"))
                .thenReturn(new SessionOutcomeTransactionService.OutcomeResult(session, billing));

        sessionService.updateSessionStatus(100L, SessionStatus.NO_SHOW.getValue(), principal, "127.0.0.1");

        verify(sessionOutcomeTransactionService).persistScheduledFallback(100L);
        verify(sessionOutcomeTransactionService).applyOutcomeAndCreateBilling(
                100L, SessionStatus.NO_SHOW.getValue(), principal, "127.0.0.1");
    }

    @Test
    @DisplayName("Cancelled status does not create billing")
    void cancelledDoesNotCreateBilling() {
        sessionService.updateSessionStatus(100L, SessionStatus.CANCELLED.getValue(), principal, "127.0.0.1");

        verify(billingService, never()).hasSessionBilling(anyLong());
        verify(billingService, never()).createSessionBilling(any(), any(), anyString());
    }

    @Test
    @DisplayName("Rescheduling status does not create billing")
    void reschedulingDoesNotCreateBilling() {
        sessionService.updateSessionStatus(100L, SessionStatus.RESCHEDULING.getValue(), principal, "127.0.0.1");

        verify(billingService, never()).hasSessionBilling(anyLong());
        verify(billingService, never()).createSessionBilling(any(), any(), anyString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"completed", "no-show"})
    @DisplayName("Cannot mark completed/no-show until after the exact scheduled time")
    void blocksOutcomeBeforeSessionDay(String status) {
        session.setSessionDate(Instant.now().plus(5, ChronoUnit.DAYS));

        assertThatThrownBy(() -> sessionService.updateSessionStatus(100L, status, principal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("scheduled time");

        verify(billingService, never()).createSessionBilling(any(), any(), anyString());
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Cancelled is allowed before the session calendar day")
    void allowsCancelBeforeSessionDay() {
        session.setSessionDate(Instant.now().plus(5, ChronoUnit.DAYS));

        sessionService.updateSessionStatus(100L, SessionStatus.CANCELLED.getValue(), principal, "127.0.0.1");

        verify(sessionRepository).save(any(Session.class));
        verify(billingService, never()).createSessionBilling(any(), any(), anyString());
    }

    @Test
    @DisplayName("Completed is allowed after the exact scheduled time")
    void allowsCompletedAfterScheduledTime() {
        session.setSessionDate(Instant.now().minus(1, ChronoUnit.SECONDS));
        SessionBillingResponse billing = SessionBillingResponse.builder().id(57L).build();
        when(sessionOutcomeTransactionService.applyOutcomeAndCreateBilling(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1"))
                .thenReturn(new SessionOutcomeTransactionService.OutcomeResult(session, billing));

        sessionService.updateSessionStatus(100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");

        verify(sessionOutcomeTransactionService).applyOutcomeAndCreateBilling(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1");
    }

    @Test
    @DisplayName("Billing failure leaves the scheduled fallback and returns a clear error")
    void billingFailureLeavesScheduledFallback() {
        when(sessionOutcomeTransactionService.applyOutcomeAndCreateBilling(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1"))
                .thenThrow(new BadRequestException("Service base rate is required"));

        assertThatThrownBy(() -> sessionService.updateSessionStatus(
                100L, SessionStatus.COMPLETED.getValue(), principal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("billing could not be generated")
                .hasMessageContaining("remains scheduled");

        verify(sessionOutcomeTransactionService).persistScheduledFallback(100L);
    }

    @Test
    @DisplayName("A session with an existing bill cannot be cancelled")
    void billedSessionCannotBeCancelled() {
        session.setBilling(SessionBilling.builder().session(session).build());

        assertThatThrownBy(() -> sessionService.updateSessionStatus(
                100L, SessionStatus.CANCELLED.getValue(), principal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("existing bill cannot be cancelled");

        verify(sessionRepository, never()).save(any(Session.class));
    }
}
