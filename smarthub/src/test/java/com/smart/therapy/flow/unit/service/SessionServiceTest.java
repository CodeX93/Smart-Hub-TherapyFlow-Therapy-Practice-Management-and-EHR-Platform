package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
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
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.session.dto.AvailabilityResponse;
import com.smart.therapy.flow.session.dto.ConflictCheckResponse;
import com.smart.therapy.flow.session.dto.CreateSessionRequest;
import com.smart.therapy.flow.session.dto.SessionResponse;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.session.repository.RoomRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.service.SessionService;
import com.smart.therapy.flow.session.service.SessionOutcomeTransactionService;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import com.smart.therapy.flow.user.repository.SupervisorAssignmentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SessionService Unit Tests")
class SessionServiceTest {

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private SupervisorAssignmentRepository supervisorAssignmentRepository;

    @Mock
    private TimezoneService timezoneService;

    @Mock
    private EmailService emailService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SessionService.ZoomService zoomService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private PermissionChecker permissionChecker;

    @Mock
    private CaseloadScopeService caseloadScopeService;

    @Mock
    private SystemOptionResolverService systemOptionResolverService;

    @Mock
    private TherapistAvailabilityService therapistAvailabilityService;

    @Mock
    private SessionOutcomeTransactionService sessionOutcomeTransactionService;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private SessionService sessionService;

    private AuthPrincipal therapistPrincipal;
    private User therapist;
    private Client client;
    private Session session;

    @BeforeEach
    void setUp() {
        therapist = TestDataFactory.createTestTherapist();
        therapist.setId(1L);
        therapistPrincipal = TestDataFactory.createAuthPrincipal(therapist);
        lenient().when(currentUserService.requireCurrentUser(any(AuthPrincipal.class))).thenReturn(therapist);
        lenient().when(systemOptionResolverService.requireOptionKey(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
        lenient().when(timezoneService.getTherapistTimezone(anyLong()))
                .thenReturn(Optional.of(ZoneId.of("America/Toronto")));
        lenient().when(caseloadScopeService.resolve(any(AuthPrincipal.class)))
                .thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(
                        CaseloadScope.ALL, List.of(), therapist.getId()));

        client = TestDataFactory.createTestClient();
        client.setId(1L);
        client.setAssignedTherapist(therapist);

        session = TestDataFactory.createTestSession(client, therapist);
        session.setId(1L);
        session.setSessionDate(LocalDate.now(ZoneId.of("America/Toronto")).plusDays(1)
                .atTime(12, 0).atZone(ZoneId.of("America/Toronto")).toInstant());
    }

    @Test
    @DisplayName("Should create session successfully when valid data provided")
    void shouldCreateSessionSuccessfully() {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(1L);
        request.setTherapistId(1L);
        request.setSessionDate(session.getSessionDate());
        request.setSessionMode("phone");
        request.setStatus("scheduled");
        request.setDuration(60);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(therapist));
        when(sessionRepository.findOverlappingTherapistSessions(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());
        when(timezoneService.getTherapistTimezone(1L)).thenReturn(java.util.Optional.of(ZoneId.of("America/New_York")));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        // Act
        SessionResponse response = sessionService.createSession(request, therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        verify(clientRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when client not found")
    void shouldThrowExceptionWhenClientNotFound() {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(999L);
        request.setTherapistId(1L);
        request.setSessionDate(session.getSessionDate());
        request.setSessionMode("phone");
        request.setDuration(60);

        when(clientRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Client not found");

        verify(clientRepository).findById(999L);
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when therapist not found")
    void shouldThrowExceptionWhenTherapistNotFound() {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(1L);
        request.setTherapistId(999L);
        request.setSessionDate(session.getSessionDate());
        request.setSessionMode("phone");
        request.setDuration(60);

        when(clientRepository.findById(1L)).thenReturn(Optional.of(client));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Therapist not found");

        verify(userRepository).findById(999L);
        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should detect room conflict when another therapist booked the same physical room")
    void shouldDetectRoomConflictForDifferentTherapist() {
        Instant sessionDate = Instant.parse("2026-07-08T14:00:00Z");
        Room room = Room.builder()
                .roomName("Office 1")
                .roomType(RoomType.PHYSICAL)
                .capacity(1)
                .build();
        room.setId(10L);

        User otherTherapist = TestDataFactory.createTestTherapist();
        otherTherapist.setId(2L);
        otherTherapist.setFullName("Other Therapist");

        Session existingSession = TestDataFactory.createTestSession(client, otherTherapist);
        existingSession.setId(2L);
        existingSession.setRoom(room);
        existingSession.setSessionDate(sessionDate);
        existingSession.setDuration(60);

        when(roomRepository.findById(10L)).thenReturn(Optional.of(room));
        when(sessionRepository.findOverlappingTherapistSessions(eq(1L), eq(sessionDate), any()))
                .thenReturn(Collections.emptyList());
        when(sessionRepository.findOverlappingRoomSessions(eq(10L), eq(sessionDate), any()))
                .thenReturn(List.of(existingSession));

        ConflictCheckResponse response = sessionService.checkConflicts(1L, 10L, sessionDate, 60);

        assertThat(response.getHasConflicts()).isTrue();
        assertThat(response.getTherapistConflicts()).isEmpty();
        assertThat(response.getRoomConflicts()).hasSize(1);
        assertThat(response.getRoomConflicts().get(0).getRoomName()).isEqualTo("Office 1");
    }

    @Test
    @DisplayName("Should throw ConflictException when session conflicts exist")
    void shouldThrowExceptionWhenSessionConflicts() {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(1L);
        request.setTherapistId(1L);
        request.setSessionDate(session.getSessionDate());
        request.setDuration(60);
        request.setSessionMode("phone");
        request.setIgnoreConflicts(false);

        Session conflictingSession = TestDataFactory.createTestSession(client, therapist);
        conflictingSession.setId(2L);

        when(timezoneService.getTherapistTimezone(1L)).thenReturn(java.util.Optional.of(ZoneId.of("America/New_York")));
        when(sessionRepository.findOverlappingTherapistSessions(anyLong(), any(), any()))
                .thenReturn(List.of(conflictingSession));

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Scheduling conflict");

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should get session by ID successfully")
    void shouldGetSessionByIdSuccessfully() {
        // Arrange
        Long sessionId = 1L;
        when(sessionRepository.findByIdWithRelations(sessionId)).thenReturn(Optional.of(session));

        // Act
        SessionResponse response = sessionService.getSession(sessionId, therapistPrincipal);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(sessionId);
        verify(sessionRepository).findByIdWithRelations(sessionId);
    }

    @Test
    @DisplayName("Should throw ResourceNotFoundException when session not found")
    void shouldThrowExceptionWhenSessionNotFound() {
        // Arrange
        Long sessionId = 999L;
        when(sessionRepository.findByIdWithRelations(sessionId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> sessionService.getSession(sessionId, therapistPrincipal))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Session not found");

        verify(sessionRepository).findByIdWithRelations(sessionId);
    }

    @Test
    @DisplayName("Should get availability for date successfully")
    void shouldGetAvailabilityForDateSuccessfully() {
        // Arrange
        LocalDate date = LocalDate.now().plusDays(1);
        Long therapistId = 1L;
        ZoneId timezone = ZoneId.of("America/New_York");

        when(timezoneService.getTherapistTimezone(therapistId)).thenReturn(java.util.Optional.of(timezone));
        when(sessionRepository.findByTherapistAndDateRange(anyLong(), any(), any()))
                .thenReturn(Collections.emptyList());

        // Act
        AvailabilityResponse response = sessionService.getAvailability(date, therapistId, null);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDate()).isEqualTo(date);
        verify(timezoneService).getTherapistTimezone(therapistId);
    }

    @Test
    @DisplayName("Should cancel session successfully")
    void shouldCancelSessionSuccessfully() {
        // Arrange
        Long sessionId = 1L;
        when(sessionRepository.findByIdWithRelations(sessionId)).thenReturn(Optional.of(session));
        when(sessionRepository.save(any(Session.class))).thenReturn(session);

        // Act
        SessionResponse response = sessionService.updateSessionStatus(sessionId, "cancelled", therapistPrincipal, "127.0.0.1");

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("cancelled");
        verify(sessionRepository).findByIdWithRelations(sessionId);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when canceling already cancelled session")
    void shouldThrowExceptionWhenCancelingCancelledSession() {
        // Arrange
        Long sessionId = 1L;
        session.setStatus(com.smart.therapy.flow.session.enums.SessionStatus.CANCELLED.getValue());
        when(sessionRepository.findByIdWithRelations(sessionId)).thenReturn(Optional.of(session));

        // Act & Assert
        assertThatThrownBy(() -> sessionService.updateSessionStatus(sessionId, "cancelled", therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be changed from cancelled to cancelled");

        verify(sessionRepository, never()).save(any(Session.class));
    }

    @Test
    @DisplayName("Should throw BadRequestException when session date is in the past")
    void shouldThrowExceptionWhenSessionDateInPast() {
        // Arrange
        CreateSessionRequest request = new CreateSessionRequest();
        request.setClientId(1L);
        request.setTherapistId(1L);
        request.setSessionDate(Instant.now().minusSeconds(3600)); // 1 hour ago
        request.setDuration(60);
        request.setSessionMode("phone");

        when(timezoneService.getTherapistTimezone(1L)).thenReturn(java.util.Optional.of(ZoneId.of("America/New_York")));

        // Act & Assert
        assertThatThrownBy(() -> sessionService.createSession(request, therapistPrincipal, "127.0.0.1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Session date cannot be in the past");

        verify(sessionRepository, never()).save(any(Session.class));
    }
}
