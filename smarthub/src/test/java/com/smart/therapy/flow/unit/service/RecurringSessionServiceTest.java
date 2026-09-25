package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.exception.ConflictException;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.session.dto.CancelRecurringSeriesResponse;
import com.smart.therapy.flow.session.dto.CreateRecurringSessionsResponse;
import com.smart.therapy.flow.session.dto.RecurrencePreviewResponse;
import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.dto.UpdateRecurringFutureRequest;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.RecurrenceDateExpander;
import com.smart.therapy.flow.session.service.RecurringSessionService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurringSessionService Unit Tests")
class RecurringSessionServiceTest {

    @Mock private SessionRepository sessionRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private UserRepository userRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private SupervisorAssignmentRepository supervisorAssignmentRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private CurrentUserService currentUserService;
    @Mock private PermissionChecker permissionChecker;
    @Mock private SubscriptionFeatureService subscriptionFeatureService;
    @Mock private TimezoneService timezoneService;
    @Spy private RecurrenceDateExpander recurrenceDateExpander = new RecurrenceDateExpander();
    @Mock private EntityManager entityManager;
    @Mock private Query nativeQuery;

    @Mock
    private SystemOptionResolverService systemOptionResolverService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private RecurringSessionService recurringSessionService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private com.smart.therapy.flow.billing.entity.Service billingService;
    private LocalDate futureMonday;
    private static final ZoneId TEST_ZONE = ZoneId.of("America/New_York");

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(
                therapist, "ROLE_THERAPIST", "CLIENT_VIEW_OWN", "SESSION_CREATE", "SESSION_EDIT");

        client = TestDataFactory.createTestClient(therapist);
        client.setId(10L);

        billingService = com.smart.therapy.flow.billing.entity.Service.builder()
                .serviceCode("90834")
                .serviceName("Psychotherapy 45 min")
                .duration(60)
                .baseRate(BigDecimal.valueOf(150))
                .build();
        billingService.setId(5L);

        futureMonday = nextMonday(LocalDate.now(TEST_ZONE).plusWeeks(2));

        ReflectionTestUtils.setField(recurringSessionService, "entityManager", entityManager);
    }

    @Test
    @DisplayName("Should preview recurring sessions with free dates")
    void shouldPreviewRecurringSessions() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_ALL"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_TEAM"))).thenReturn(false);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(billingService));
        when(sessionRepository.findActiveSessionsInRange(any(), any(), any())).thenReturn(List.of());
        when(timezoneService.normalizeTimezoneId(anyString())).thenAnswer(invocation ->
                ZoneId.of(invocation.getArgument(0)).getId());
        RecurrenceRuleRequest request = weeklyRule(3);

        RecurrencePreviewResponse response = recurringSessionService.previewRecurringSessions(
                request, therapistPrincipal);

        assertThat(response.getTotalRequested()).isEqualTo(3);
        assertThat(response.getConflictCount()).isZero();
        assertThat(response.getFreeCount()).isEqualTo(3);
        assertThat(response.getSessions()).hasSize(3);
        assertThat(response.getSessions()).allMatch(s -> !s.isHasConflict());
    }

    @Test
    @DisplayName("Should create recurring series with shared group id")
    void shouldCreateRecurringSeries() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(entityManager.createNativeQuery(anyString())).thenReturn(nativeQuery);
        when(nativeQuery.getSingleResult()).thenReturn(1L);
        when(nativeQuery.setParameter(eq(1), anyLong())).thenReturn(nativeQuery);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_ALL"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_TEAM"))).thenReturn(false);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(billingService));
        when(sessionRepository.findActiveSessionsInRange(any(), any(), any())).thenReturn(List.of());
        when(timezoneService.normalizeTimezoneId(anyString())).thenAnswer(invocation ->
                ZoneId.of(invocation.getArgument(0)).getId());
        RecurrenceRuleRequest request = weeklyRule(2);
        request.setTherapistId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));

        AtomicLong idSeq = new AtomicLong(100);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session saved = invocation.getArgument(0);
            saved.setId(idSeq.getAndIncrement());
            return saved;
        });

        CreateRecurringSessionsResponse response = recurringSessionService.createRecurringSessions(
                request, therapistPrincipal, "127.0.0.1");

        assertThat(response.getGroupId()).startsWith("rec-");
        assertThat(response.getCreatedCount()).isEqualTo(2);
        assertThat(response.getSkippedCount()).isZero();
        assertThat(response.getCreated()).hasSize(2);
        assertThat(response.getCreated()).extracting("recurrenceGroupId").containsOnly(response.getGroupId());
        verify(sessionRepository, times(2)).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw conflict when all dates overlap existing sessions")
    void shouldRejectWhenAllDatesConflict() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_ALL"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_TEAM"))).thenReturn(false);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(billingService));
        when(sessionRepository.findActiveSessionsInRange(any(), any(), any())).thenReturn(List.of());
        when(timezoneService.normalizeTimezoneId(anyString())).thenAnswer(invocation ->
                ZoneId.of(invocation.getArgument(0)).getId());
        RecurrenceRuleRequest request = weeklyRule(1);
        request.setTherapistId(1L);

        Session blocking = TestDataFactory.createTestSession(client, therapist);
        blocking.setDuration(60);
        blocking.setStatus(SessionStatus.SCHEDULED.getValue());
        blocking.setSessionDate(recurrenceDateExpander.expand(request, TEST_ZONE).get(0).utcDate());
        when(sessionRepository.findActiveSessionsInRange(any(), any(), any())).thenReturn(List.of(blocking));

        assertThatThrownBy(() -> recurringSessionService.createRecurringSessions(
                request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("All recurrence dates conflict");

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should cancel upcoming sessions in a series")
    void shouldCancelRecurringSeries() {
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_ALL"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_TEAM"))).thenReturn(false);
        String groupId = "rec-test-group";
        Instant future = Instant.now().plus(3, ChronoUnit.DAYS);

        Session upcoming = TestDataFactory.createTestSession(client, therapist);
        upcoming.setId(50L);
        upcoming.setRecurrenceGroupId(groupId);
        upcoming.setSessionDate(future);
        upcoming.setStatus(SessionStatus.SCHEDULED.getValue());
        upcoming.setService(billingService);

        Session past = TestDataFactory.createTestSession(client, therapist);
        past.setId(51L);
        past.setRecurrenceGroupId(groupId);
        past.setSessionDate(Instant.now().minus(3, ChronoUnit.DAYS));
        past.setStatus(SessionStatus.COMPLETED.getValue());
        past.setService(billingService);

        when(sessionRepository.findByRecurrenceGroupId(groupId)).thenReturn(List.of(upcoming, past));
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CancelRecurringSeriesResponse response = recurringSessionService.cancelRecurringSeries(
                groupId, therapistPrincipal, "127.0.0.1");

        assertThat(response.getGroupId()).isEqualTo(groupId);
        assertThat(response.getCancelledCount()).isEqualTo(1);
        assertThat(upcoming.getStatus()).isEqualTo(SessionStatus.CANCELLED.getValue());
        assertThat(past.getStatus()).isEqualTo(SessionStatus.COMPLETED.getValue());
    }

    @Test
    @DisplayName("Should reject invalid recurrence group id on update")
    void shouldRejectInvalidGroupId() {
        UpdateRecurringFutureRequest request = new UpdateRecurringFutureRequest();
        request.setAnchorId(1L);
        request.setSessionDate(Instant.now().plus(5, ChronoUnit.DAYS));

        assertThatThrownBy(() -> recurringSessionService.updateFutureRecurringSessions(
                "bad-group", request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid recurrence group id");
    }

    @Test
    @DisplayName("Should reject anchor not in series")
    void shouldRejectAnchorNotInSeries() {
        String groupId = "rec-test-group";
        Session anchor = TestDataFactory.createTestSession(client, therapist);
        anchor.setId(70L);
        anchor.setRecurrenceGroupId("rec-other-group");
        anchor.setService(billingService);

        when(sessionRepository.findByIdWithRelations(70L)).thenReturn(Optional.of(anchor));

        UpdateRecurringFutureRequest request = new UpdateRecurringFutureRequest();
        request.setAnchorId(70L);
        request.setSessionDate(Instant.now().plus(5, ChronoUnit.DAYS));

        assertThatThrownBy(() -> recurringSessionService.updateFutureRecurringSessions(
                groupId, request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not part of this series");
    }

    @Test
    @DisplayName("Should reject business hours outside practice window")
    void shouldRejectOutsideBusinessHours() {
        when(clientRepository.findById(10L)).thenReturn(Optional.of(client));
        when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_ALL"))).thenReturn(false);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_OWN"))).thenReturn(true);
        when(permissionChecker.hasPermission(any(), eq("CLIENT_VIEW_TEAM"))).thenReturn(false);
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(billingService));
        when(timezoneService.normalizeTimezoneId(anyString())).thenAnswer(invocation ->
                ZoneId.of(invocation.getArgument(0)).getId());
        RecurrenceRuleRequest request = weeklyRule(1);
        request.setSessionDate(futureMonday.atTime(6, 0).atZone(TEST_ZONE).toInstant());

        assertThatThrownBy(() -> recurringSessionService.previewRecurringSessions(
                request, therapistPrincipal))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("before 8:00 AM");
    }

    @Test
    @DisplayName("Should return 404 when recurring series not found on cancel")
    void shouldReturnNotFoundWhenSeriesMissing() {
        when(sessionRepository.findByRecurrenceGroupId("rec-missing")).thenReturn(List.of());

        assertThatThrownBy(() -> recurringSessionService.cancelRecurringSeries(
                "rec-missing", therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recurring series not found");
    }

    private RecurrenceRuleRequest weeklyRule(int count) {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest();
        request.setClientId(10L);
        request.setTherapistId(1L);
        request.setServiceId(5L);
        request.setSessionMode("in_person");
        request.setSessionType("Psychotherapy");
        request.setSessionDate(futureMonday.atTime(14, 0).atZone(TEST_ZONE).toInstant());
        request.setTimezone(TEST_ZONE.getId());
        request.setRecurrenceType(RecurrenceType.WEEKLY);
        request.setDaysOfWeek(List.of(futureMonday.getDayOfWeek().getValue() % 7));
        request.setInterval(1);
        request.setEndMode(RecurrenceEndMode.COUNT);
        request.setCount(count);
        return request;
    }

    private static LocalDate nextMonday(LocalDate from) {
        LocalDate cursor = from;
        while (cursor.getDayOfWeek().getValue() % 7 != 1) {
            cursor = cursor.plusDays(1);
        }
        return cursor;
    }
}
