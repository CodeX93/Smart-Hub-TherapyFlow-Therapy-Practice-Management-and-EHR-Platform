package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionChecker;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client delete closes open schedule sessions")
class ClientDeleteCascadeSessionsTest {

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private CurrentUserService currentUserService;
    @Mock
    private PermissionChecker permissionChecker;
    @Mock
    private AuditLogService auditLogService;
    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ClientService clientService;

    private AuthPrincipal adminPrincipal;
    private User admin;

    @BeforeEach
    void setUp() {
        admin = TestDataFactory.createTestAdmin();
        admin.setId(2L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);
        when(permissionChecker.hasPermission(adminPrincipal, "CLIENT_DELETE")).thenReturn(true);
        when(currentUserService.requireCurrentUser(adminPrincipal)).thenReturn(admin);
    }

    @Test
    @DisplayName("Cancels and soft-deletes open sessions when client is deleted")
    void closesOpenSessionsOnClientDelete() {
        Long clientId = 2254L;
        Client client = TestDataFactory.createTestClientWithId(clientId);
        client.setIsDeleted(false);
        client.setHistory(List.of());
        client.setClientId("CL-WEB-TEST2254");
        client.setStatus("active");
        client.setStage("therapy");

        Session open = TestDataFactory.createTestSession();
        open.setId(8031L);
        open.setStatus("scheduled");
        open.setIsDeleted(false);

        Session completed = TestDataFactory.createTestSession();
        completed.setId(9000L);
        completed.setStatus("completed");
        completed.setIsDeleted(false);

        when(clientRepository.findByIdIncludingDeleted(clientId)).thenReturn(Optional.of(client));
        when(clientRepository.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));
        when(sessionRepository.findByClientId(clientId)).thenReturn(List.of(open, completed));
        when(sessionRepository.save(any(Session.class))).thenAnswer(inv -> inv.getArgument(0));

        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0.0.0";
        clientService.deleteClient(clientId, adminPrincipal, "127.0.0.1", userAgent);

        assertThat(client.getIsDeleted()).isTrue();
        assertThat(client.getDeletedAt()).isNotNull();
        assertThat(client.getNextAppointmentDate()).isNull();

        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        verify(sessionRepository, atLeastOnce()).save(sessionCaptor.capture());
        Session closed = sessionCaptor.getAllValues().stream()
                .filter(s -> Long.valueOf(8031L).equals(s.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(closed.getStatus()).isEqualTo("cancelled");
        assertThat(closed.getIsDeleted()).isTrue();
        assertThat(closed.getDeletedAt()).isNotNull();

        verify(sessionRepository, never()).save(eq(completed));
        verify(clientRepository).flush();
        ArgumentCaptor<String> detailsCaptor = ArgumentCaptor.forClass(String.class);
        verify(auditLogService).recordStaffEvent(
                eq(admin.getId()),
                eq("client_deleted"),
                eq("client"),
                eq(clientId),
                eq(clientId),
                eq("127.0.0.1"),
                eq(true),
                detailsCaptor.capture(),
                eq(userAgent));
        assertThat(detailsCaptor.getValue()).contains("internal_client_id");
        assertThat(detailsCaptor.getValue()).contains(String.valueOf(clientId));
        assertThat(detailsCaptor.getValue()).contains("CL-WEB-TEST2254");
        assertThat(detailsCaptor.getValue()).contains("device_fingerprint");
        assertThat(detailsCaptor.getValue()).contains("device_label");
    }
}
