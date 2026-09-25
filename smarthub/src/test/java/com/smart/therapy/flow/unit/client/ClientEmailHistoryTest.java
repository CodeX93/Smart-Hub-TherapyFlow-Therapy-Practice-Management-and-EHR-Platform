package com.smart.therapy.flow.unit.client;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.client.dto.ClientHistoryListResponse;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.repository.ClientHistoryRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientService;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.CaseloadScope;
import com.smart.therapy.flow.common.security.CaseloadScopeService;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationDeliveryLog;
import com.smart.therapy.flow.notification.enums.NotificationChannel;
import com.smart.therapy.flow.notification.enums.NotificationStatus;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.repository.NotificationDeliveryLogRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Client email history (communications log)")
class ClientEmailHistoryTest {

    private static final List<NotificationStatus> SUCCESSFUL_EMAIL_STATUSES = List.of(
            NotificationStatus.SENT,
            NotificationStatus.DELIVERED,
            NotificationStatus.READ);

    @Mock
    private ClientRepository clientRepository;
    @Mock
    private ClientHistoryRepository clientHistoryRepository;
    @Mock
    private SessionRepository sessionRepository;
    @Mock
    private SessionBillingRepository sessionBillingRepository;
    @Mock
    private NotificationDeliveryLogRepository notificationDeliveryLogRepository;
    @Mock
    private CaseloadScopeService caseloadScopeService;

    @InjectMocks
    private ClientService clientService;

    private AuthPrincipal adminPrincipal;
    private Client client;

    @BeforeEach
    void setUp() {
        User admin = TestDataFactory.createTestAdmin();
        admin.setId(2L);
        adminPrincipal = TestDataFactory.createAuthPrincipal(admin);

        client = Client.builder().clientId("CL-2025-1270").fullName("Fahed Abdo").build();
        client.setId(1270L);

        when(caseloadScopeService.resolve(any(AuthPrincipal.class)))
                .thenReturn(new CaseloadScopeService.ResolvedCaseloadScope(CaseloadScope.ALL, List.of(), null));
    }

    @Test
    @DisplayName("Returns related client/session/billing notifications like ClientHub communications log")
    void returnsNotificationsAsEmailHistory() {
        when(clientRepository.findById(1270L)).thenReturn(Optional.of(client));
        when(notificationDeliveryLogRepository.findSuccessfulByRelatedEntity(
                eq("client"), eq(1270L), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of(delivery(10L, notification(
                        110L,
                        NotificationType.APPOINTMENT_REMINDER,
                        "Appointment Reminder",
                        "You have an upcoming appointment.",
                        "client",
                        1270L,
                        Instant.parse("2026-09-08T13:59:00Z")),
                        Instant.parse("2026-09-08T14:00:00Z"))));
        when(notificationDeliveryLogRepository.findSuccessfulByClientAndChannel(
                eq(client), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of());
        when(sessionRepository.findIdsByClientId(1270L)).thenReturn(List.of(55L));
        when(notificationDeliveryLogRepository.findSuccessfulByRelatedEntities(
                eq("session"), eq(List.of(55L)), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of(delivery(20L, notification(
                        120L,
                        NotificationType.APPOINTMENT_CANCELLED,
                        "Session Cancelled",
                        "An appointment was cancelled.",
                        "session",
                        55L,
                        Instant.parse("2026-09-07T09:59:00Z")),
                        Instant.parse("2026-09-07T10:00:00Z"))));
        when(sessionBillingRepository.findIdsByClientId(1270L)).thenReturn(List.of(88L));
        when(notificationDeliveryLogRepository.findSuccessfulByRelatedEntities(
                eq("billing"), eq(List.of(88L)), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of(delivery(30L, notification(
                        130L,
                        NotificationType.INVOICE_GENERATED,
                        "Invoice Sent - INV-CL-2025-1270-88",
                        "Invoice sent for session billing.",
                        "billing",
                        88L,
                        Instant.parse("2026-09-06T11:59:00Z")),
                        Instant.parse("2026-09-06T12:00:00Z"))));
        when(clientHistoryRepository.findByClientIdOrderByCreatedAtDesc(1270L)).thenReturn(List.of());

        ClientHistoryListResponse response = clientService.getClientEmailHistory(1270L, adminPrincipal);

        assertThat(response.getCount()).isEqualTo(3);
        assertThat(response.getHistory()).extracting("id").containsExactly(10L, 20L, 30L);
        assertThat(response.getHistory().get(0).getEventType()).isEqualTo("Appointment Reminder");
        assertThat(response.getHistory().get(0).getEventSource()).isEqualTo("APPOINTMENT_REMINDER");
        assertThat(response.getHistory().get(0).getCreatedAt()).isEqualTo(Instant.parse("2026-09-08T14:00:00Z"));
        assertThat(response.getHistory().get(2).getFromValue()).isEqualTo("billing");
        assertThat(response.getHistory().get(2).getToValue()).isEqualTo("88");
        assertThat(response.getMessage()).isNull();
    }

    @Test
    @DisplayName("Returns empty message when no communications exist")
    void returnsEmptyMessageWhenNoCommunications() {
        when(clientRepository.findById(1270L)).thenReturn(Optional.of(client));
        when(notificationDeliveryLogRepository.findSuccessfulByRelatedEntity(
                eq("client"), eq(1270L), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of());
        when(notificationDeliveryLogRepository.findSuccessfulByClientAndChannel(
                eq(client), eq(NotificationChannel.EMAIL), eq(SUCCESSFUL_EMAIL_STATUSES)))
                .thenReturn(List.of());
        when(sessionRepository.findIdsByClientId(1270L)).thenReturn(List.of());
        when(sessionBillingRepository.findIdsByClientId(1270L)).thenReturn(List.of());
        when(clientHistoryRepository.findByClientIdOrderByCreatedAtDesc(1270L)).thenReturn(List.of());

        ClientHistoryListResponse response = clientService.getClientEmailHistory(1270L, adminPrincipal);

        assertThat(response.getCount()).isZero();
        assertThat(response.getHistory()).isEmpty();
        assertThat(response.getMessage()).contains("No email history");
    }

    private static Notification notification(
            Long id,
            NotificationType type,
            String title,
            String message,
            String relatedType,
            Long relatedId,
            Instant createdAt) {
        Notification notification = Notification.builder()
                .type(type)
                .title(title)
                .message(message)
                .relatedEntityType(relatedType)
                .relatedEntityId(relatedId)
                .build();
        notification.setId(id);
        notification.setCreatedAt(createdAt);
        return notification;
    }

    private static NotificationDeliveryLog delivery(Long id, Notification notification, Instant sentAt) {
        return NotificationDeliveryLog.builder()
                .id(id)
                .notification(notification)
                .channel(NotificationChannel.EMAIL)
                .status(NotificationStatus.SENT)
                .sentAt(sentAt)
                .createdAt(sentAt)
                .build();
    }
}
