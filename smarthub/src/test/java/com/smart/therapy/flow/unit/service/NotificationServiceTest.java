package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.TestDataFactory;
import com.smart.therapy.flow.common.exception.ResourceNotFoundException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.notification.dto.NotificationResponse;
import com.smart.therapy.flow.notification.dto.NotificationTriggerRequest;
import com.smart.therapy.flow.notification.dto.NotificationTriggerResponse;
import com.smart.therapy.flow.notification.entity.Notification;
import com.smart.therapy.flow.notification.entity.NotificationTrigger;
import com.smart.therapy.flow.notification.repository.NotificationRepository;
import com.smart.therapy.flow.notification.repository.NotificationTemplateRepository;
import com.smart.therapy.flow.notification.repository.NotificationTriggerRepository;
import com.smart.therapy.flow.notification.repository.ScheduledNotificationRepository;
import com.smart.therapy.flow.notification.service.NotificationService;
import com.smart.therapy.flow.notification.service.NotificationService.EmailService;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationService Unit Tests")
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private NotificationTriggerRepository triggerRepository;

    @Mock
    private NotificationTemplateRepository templateRepository;

    @Mock
    private ScheduledNotificationRepository scheduledNotificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private OptionCategoryRepository optionCategoryRepository;

    @InjectMocks
    private NotificationService notificationService;

    private AuthPrincipal userPrincipal;
    private User user;
    private Notification notification;
    private NotificationTrigger trigger;

    @BeforeEach
    void setUp() {
        user = TestDataFactory.createTestTherapist();
        user.setId(1L);
        userPrincipal = TestDataFactory.createAuthPrincipal(user);

        notification = Notification.builder()
                .user(user)
                .title("Test Notification")
                .message("This is a test notification")
                .isRead(false)
                .build();

        trigger = NotificationTrigger.builder()
                .name("Session Reminder")
                .eventType("session_created")
                .isActive(true)
                .build();

    }

    /**
     * Wires a real TimezoneService so the assertions exercise genuine formatting:
     * therapist in America/New_York, client in Asia/Karachi, clinic in UTC.
     */
    private com.smart.therapy.flow.client.repository.ClientRepository wireTwoRecipientsInDifferentZones(
            com.smart.therapy.flow.client.entity.Client client) {
        var clients = mock(com.smart.therapy.flow.client.repository.ClientRepository.class);
        var profiles = mock(com.smart.therapy.flow.user.repository.UserProfileRepository.class);
        var practiceConfig = mock(
                com.smart.therapy.flow.system.repository.PracticeConfigurationRepository.class);

        client.setTimezone("Asia/Karachi");
        when(clients.findById(7L)).thenReturn(Optional.of(client));
        when(profiles.findByUserId(1L)).thenReturn(Optional.of(
                com.smart.therapy.flow.user.entity.UserProfile.builder()
                        .timezone("America/New_York").build()));

        var organisations = mock(
                com.smart.therapy.flow.organisation.repository.OrganisationRepository.class);
        var realTimezoneService = new com.smart.therapy.flow.common.service.TimezoneService(
                profiles, clients, practiceConfig, organisations);
        org.springframework.test.util.ReflectionTestUtils.setField(
                notificationService, "timezoneService", realTimezoneService);
        return clients;
    }

    private void stubSessionScheduledTemplates(String bodyTemplate) {
        trigger.setEventType("session_scheduled");
        trigger.setRecipientRules("{\"assignedTherapist\":true,\"sessionClient\":true}");
        trigger.setIsScheduled(false);
        when(triggerRepository.findByEventType("session_scheduled")).thenReturn(List.of(trigger));
        var email = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("email").isActive(true).subject("Booked")
                .bodyTemplate(bodyTemplate).build();
        when(templateRepository.findByEventTypeIgnoreCase("session_scheduled")).thenReturn(List.of(email));
    }

    @Test
    void sessionEmailRendersEachRecipientsOwnTimezone() {
        var contacts = mock(com.smart.therapy.flow.client.service.ClientContactService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientContactService", contacts);
        when(contacts.getPrimaryEmail(7L)).thenReturn(Optional.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder()
                        .contactValue("booking-client@example.com").build()));
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        var client = TestDataFactory.createTestClient();
        client.setId(7L);
        client.setAuthIdentity(com.smart.therapy.flow.auth.entity.AuthIdentity.builder()
                .email("booking-client@example.com").build());
        var clients = wireTwoRecipientsInDifferentZones(client);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        stubSessionScheduledTemplates("{{sessionTimeRangeFormatted}} | {{sessionDateOnlyFormatted}}");

        // 18:00 UTC = 2:00 PM in New York (EDT), 11:00 PM in Karachi.
        var payload = new java.util.HashMap<String, Object>();
        payload.put("clientId", 7L);
        payload.put("therapistId", 1L);
        payload.put("id", 123L);
        payload.put("sessionDate", Instant.parse("2026-09-23T18:00:00Z"));
        payload.put("duration", 60);

        notificationService.processEvent("session_scheduled", payload);

        verify(sender).sendEmail(eq("booking-client@example.com"), anyString(),
                contains("11:00 PM - 12:00 AM"));
        verify(sender).sendEmail(eq("booking-client@example.com"), anyString(),
                contains("(Asia/Karachi)"));
        verify(sender).sendEmail(eq(user.getEmail()), anyString(),
                contains("2:00 PM - 3:00 PM"));
        verify(sender).sendEmail(eq(user.getEmail()), anyString(),
                contains("(America/New_York)"));
        // Neither recipient is shown the other's timezone.
        verify(sender, never()).sendEmail(eq(user.getEmail()), anyString(), contains("Asia/Karachi"));
        verify(sender, never()).sendEmail(eq("booking-client@example.com"), anyString(),
                contains("America/New_York"));
    }

    @Test
    void scheduledReminderLocalizesASerializedSessionDateAtDeliveryTime() {
        var contacts = mock(com.smart.therapy.flow.client.service.ClientContactService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientContactService", contacts);
        when(contacts.getPrimaryEmail(7L)).thenReturn(Optional.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder()
                        .contactValue("booking-client@example.com").build()));
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        var client = TestDataFactory.createTestClient();
        client.setId(7L);
        client.setAuthIdentity(com.smart.therapy.flow.auth.entity.AuthIdentity.builder()
                .email("booking-client@example.com").build());
        var clients = wireTwoRecipientsInDifferentZones(client);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        stubSessionScheduledTemplates("{{sessionTimeRangeFormatted}}");

        // A reminder's payload comes back from the DB as JSON: the instant is a String,
        // and the duration may arrive as a String too.
        var payload = new java.util.HashMap<String, Object>();
        payload.put("clientId", 7L);
        payload.put("therapistId", 1L);
        payload.put("id", 123L);
        payload.put("sessionDate", "2026-09-23T18:00:00Z");
        payload.put("duration", "60");

        notificationService.processEvent("session_scheduled", payload);

        verify(sender).sendEmail(eq("booking-client@example.com"), anyString(),
                contains("11:00 PM - 12:00 AM PKT (Asia/Karachi)"));
        verify(sender).sendEmail(eq(user.getEmail()), anyString(),
                contains("2:00 PM - 3:00 PM EDT (America/New_York)"));
    }

    @Test
    void eventWithoutASessionInstantKeepsTheCallersFormattedValues() {
        var contacts = mock(com.smart.therapy.flow.client.service.ClientContactService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientContactService", contacts);
        when(contacts.getPrimaryEmail(7L)).thenReturn(Optional.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder()
                        .contactValue("booking-client@example.com").build()));
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        var client = TestDataFactory.createTestClient();
        client.setId(7L);
        client.setAuthIdentity(com.smart.therapy.flow.auth.entity.AuthIdentity.builder()
                .email("booking-client@example.com").build());
        var clients = wireTwoRecipientsInDifferentZones(client);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        stubSessionScheduledTemplates("{{sessionTimeRangeFormatted}}");

        var payload = new java.util.HashMap<String, Object>();
        payload.put("clientId", 7L);
        payload.put("therapistId", 1L);
        payload.put("id", 123L);
        payload.put("sessionTimeRangeFormatted", "caller supplied range");

        notificationService.processEvent("session_scheduled", payload);

        verify(sender, times(2)).sendEmail(anyString(), anyString(), contains("caller supplied range"));
    }

    @Test
    void bookingWorkflowSendsOneEmailAndOneInAppNotificationPerRecipientDespiteDuplicateTriggers() {
        var clients = mock(com.smart.therapy.flow.client.repository.ClientRepository.class);
        var contacts = mock(com.smart.therapy.flow.client.service.ClientContactService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientContactService", contacts);
        when(contacts.getPrimaryEmail(7L)).thenReturn(Optional.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder().contactValue("booking-client@example.com").build()));
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        var client = TestDataFactory.createTestClient();
        client.setId(7L);
        client.setAuthIdentity(com.smart.therapy.flow.auth.entity.AuthIdentity.builder()
                .email("booking-client@example.com").build());
        when(clients.findById(7L)).thenReturn(Optional.of(client));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        trigger.setEventType("session_scheduled");
        trigger.setRecipientRules("{\"assignedTherapist\":true,\"sessionClient\":true}");
        trigger.setIsScheduled(false);
        when(triggerRepository.findByEventType("session_scheduled")).thenReturn(List.of(trigger, trigger));
        var email = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("email").isActive(true).subject("Booked")
                .bodyTemplate("{{sessionDetailsHtml}} {{calendarUrl}} {{sessionUrl}}").build();
        var inApp = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("in_app").isActive(true).subject("Booked").bodyTemplate("Session booked").build();
        when(templateRepository.findByEventTypeIgnoreCase("session_scheduled")).thenReturn(List.of(email, inApp));

        notificationService.processEvent("session_scheduled", Map.of(
                "clientId", 7L, "therapistId", 1L, "id", 123L,
                "sessionDetailsHtml", "Appointment details", "calendarUrl", "https://calendar.google.com/calendar/render"));

        verify(sender).sendEmail(eq("booking-client@example.com"), eq("Booked"),
                contains("/user/booked-sessions"));
        verify(sender).sendEmail(eq(user.getEmail()), eq("Booked"), contains("/therapist/scheduling"));
        verify(sender, times(2)).sendEmail(anyString(), anyString(), contains("Appointment details"));
        verify(notificationRepository).save(argThat(n -> n.getClient() == client));
        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void aliasCommentEventOnlyReachesRecipientsTheCanonicalEventDidNotClaim() {
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        var extraRecipient = TestDataFactory.createTestTherapist();
        extraRecipient.setId(2L);
        extraRecipient.setEmail("supervisor@example.com");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.findById(2L)).thenReturn(Optional.of(extraRecipient));

        var canonicalTrigger = NotificationTrigger.builder()
                .name("Task Comment Added Trigger").eventType("task_comment_added")
                .recipientRules("{\"assignedTherapist\":true}").isActive(true).isScheduled(false).build();
        // A tenant-customized alias trigger: it resolves the assignee (already claimed by the
        // canonical event) plus one extra recipient only this trigger knows about.
        var aliasTrigger = NotificationTrigger.builder()
                .name("Comment Added Trigger").eventType("comment_added")
                .recipientRules("{\"assignedTherapist\":true,\"specificUsers\":[2]}")
                .isActive(true).isScheduled(false).build();
        when(triggerRepository.findByEventType("task_comment_added")).thenReturn(List.of(canonicalTrigger));
        when(triggerRepository.findByEventType("comment_added")).thenReturn(List.of(aliasTrigger));

        var email = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("email").isActive(true).subject("Task Comment")
                .bodyTemplate("{{authorName}} commented").build();
        var inApp = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("in_app").isActive(true).subject("Task Comment")
                .bodyTemplate("{{authorName}} commented").build();
        when(templateRepository.findByEventTypeIgnoreCase("task_comment_added")).thenReturn(List.of(email, inApp));
        when(templateRepository.findByEventTypeIgnoreCase("comment_added")).thenReturn(List.of(email, inApp));

        notificationService.processEvents(
                List.of("task_comment_added", "comment_added"),
                Map.of("therapistId", 1L, "id", 42L, "authorName", "Dr. Author"));

        // The assignee is claimed by the canonical event and must not get a second copy from
        // the alias; the alias still delivers to the recipient only its own trigger resolves.
        verify(sender, times(1)).sendEmail(eq(user.getEmail()), anyString(), anyString());
        verify(sender, times(1)).sendEmail(eq("supervisor@example.com"), anyString(), anyString());
        verify(notificationRepository, times(1)).save(argThat(n -> n.getUser() == user));
        verify(notificationRepository, times(1)).save(argThat(n -> n.getUser() == extraRecipient));
        verify(notificationRepository, times(2)).save(any());
    }

    @Test
    void scheduledPollProcessesPendingRowWithLargeEntityData() {
        var em = mock(jakarta.persistence.EntityManager.class);
        var tableCheck = mock(jakarta.persistence.Query.class);
        when(em.createNativeQuery(anyString())).thenReturn(tableCheck);
        when(tableCheck.getSingleResult()).thenReturn("scheduled_notifications");
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "entityManager", em);

        trigger.setEventType("session_reminder");
        trigger.setRecipientRules("{\"assignedTherapist\":true}");
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        var inApp = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("in_app").isActive(true).subject("Reminder").bodyTemplate("Session soon").build();
        when(templateRepository.findByEventTypeIgnoreCase("session_reminder")).thenReturn(List.of(inApp));

        // A payload the size production stores: hundreds of KB of serialized session data.
        String largePayload = "{\"therapistId\":1,\"note\":\"" + "x".repeat(400_000) + "\"}";
        var scheduled = com.smart.therapy.flow.notification.entity.ScheduledNotification.builder()
                .status(com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus.PENDING)
                .executeAt(Instant.now().minusSeconds(60))
                .trigger(trigger)
                .entityData(largePayload)
                .build();
        when(scheduledNotificationRepository.findByStatusAndExecuteAtBefore(
                eq(com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus.PENDING), any(Instant.class)))
                .thenReturn(List.of(scheduled));

        org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                notificationService, "processScheduledNotificationsForTenant");

        assertThat(scheduled.getStatus())
                .isEqualTo(com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus.COMPLETED);
        assertThat(scheduled.getProcessedAt()).isNotNull();
        assertThat(scheduled.getLastError()).isNull();
        assertThat(scheduled.getRetryCount()).isZero();
        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
        verify(scheduledNotificationRepository).saveAll(List.of(scheduled));
        // The poll only ever asks for PENDING rows; FAILED rows stay parked.
        verify(scheduledNotificationRepository).findByStatusAndExecuteAtBefore(
                eq(com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus.PENDING), any(Instant.class));
    }

    @Test
    @DisplayName("Should get user notifications successfully")
    void shouldGetUserNotificationsSuccessfully() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndIsDeletedOrderByCreatedAtDesc(userId, false))
                .thenReturn(List.of(notification));

        // Act
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId, false);

        // Assert
        assertThat(notifications).isNotNull();
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getTitle()).isEqualTo("Test Notification");
        verify(notificationRepository).findByUserIdAndIsDeletedOrderByCreatedAtDesc(userId, false);
    }

    @Test
    @DisplayName("Should get unread notifications only")
    void shouldGetUnreadNotificationsOnly() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false))
                .thenReturn(List.of(notification));

        // Act
        List<NotificationResponse> notifications = notificationService.getUserNotifications(userId, true);

        // Assert
        assertThat(notifications).isNotNull();
        assertThat(notifications).hasSize(1);
        verify(notificationRepository).findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false);
    }

    @Test
    @DisplayName("Should mark notification as read successfully")
    void shouldMarkNotificationAsReadSuccessfully() {
        // Arrange
        Long notificationId = 1L;
        Long userId = 1L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(notification);

        // Act
        Instant before = Instant.now();
        notificationService.markAsRead(notificationId, userId);

        // Assert
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isBetween(before, Instant.now());
        verify(notificationRepository).findById(notificationId);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw exception when notification not found")
    void shouldThrowExceptionWhenNotificationNotFound() {
        // Arrange
        Long notificationId = 999L;
        Long userId = 1L;
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notification not found");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should throw exception when notification belongs to different user")
    void shouldThrowExceptionWhenNotificationBelongsToDifferentUser() {
        // Arrange
        Long notificationId = 1L;
        Long differentUserId = 999L;
        User differentUser = TestDataFactory.createTestTherapist();
        differentUser.setId(999L);
        notification.setUser(differentUser);

        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(notification));

        // Act & Assert
        assertThatThrownBy(() -> notificationService.markAsRead(notificationId, 1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Notification does not belong to user");

        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    @DisplayName("Should mark all notifications as read successfully")
    void shouldMarkAllNotificationsAsReadSuccessfully() {
        // Arrange
        Long userId = 1L;
        Notification unreadNotification = Notification.builder()
                .user(user)
                .title("Another Notification")
                .isRead(false)
                .build();

        when(notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false))
                .thenReturn(List.of(notification, unreadNotification));
        when(notificationRepository.saveAll(anyList())).thenReturn(List.of(notification, unreadNotification));

        // Act
        Instant before = Instant.now();
        notificationService.markAllAsRead(userId);

        // Assert
        assertThat(notification.getIsRead()).isTrue();
        assertThat(notification.getReadAt()).isBetween(before, Instant.now());
        assertThat(unreadNotification.getIsRead()).isTrue();
        verify(notificationRepository).findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false);
        verify(notificationRepository).saveAll(anyList());
    }

    @Test
    @DisplayName("Should get unread count successfully")
    void shouldGetUnreadCountSuccessfully() {
        // Arrange
        Long userId = 1L;
        when(notificationRepository.findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false))
                .thenReturn(List.of(notification));

        // Act
        long count = notificationService.getUnreadCount(userId);

        // Assert
        assertThat(count).isEqualTo(1);
        verify(notificationRepository).findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(userId, false, false);
    }

    @Test
    @DisplayName("Should process an event without delivering when no recipients resolve")
    void shouldProcessEventSuccessfully() {
        // Arrange
        String eventType = "session_created";
        Map<String, Object> entityData = new HashMap<>();
        entityData.put("sessionId", 1L);
        entityData.put("clientId", 1L);

        when(triggerRepository.findByEventType(eventType)).thenReturn(List.of(trigger));

        // Act
        notificationService.processEvent(eventType, entityData);

        // Assert
        verify(triggerRepository).findByEventType(eventType);
        verifyNoInteractions(notificationRepository, emailService);
    }

    @Test
    @DisplayName("Should create trigger successfully")
    void shouldCreateTriggerSuccessfully() {
        // Arrange
        NotificationTriggerRequest request = new NotificationTriggerRequest();
        request.setName("New Trigger");
        request.setDescription("Notify when a new event occurs");
        request.setEventType("new_event");
        request.setPriority("HIGH");
        request.setBatchWindowMinutes(15);
        request.setMaxBatchSize(25);
        request.setIsActive(true);

        when(triggerRepository.save(any(NotificationTrigger.class))).thenAnswer(invocation -> {
            NotificationTrigger saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        // Act
        NotificationTriggerResponse response = notificationService.createTrigger(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getDescription()).isEqualTo("Notify when a new event occurs");
        assertThat(response.getBatchWindowMinutes()).isEqualTo(15);
        assertThat(response.getMaxBatchSize()).isEqualTo(25);
        verify(triggerRepository).save(any(NotificationTrigger.class));
    }

    @Test
    @DisplayName("Should update trigger successfully")
    void shouldUpdateTriggerSuccessfully() {
        // Arrange
        Long triggerId = 1L;
        NotificationTriggerRequest request = new NotificationTriggerRequest();
        request.setName("Updated Trigger Name");
        request.setIsActive(false);

        when(triggerRepository.findById(triggerId)).thenReturn(Optional.of(trigger));
        when(triggerRepository.save(any(NotificationTrigger.class))).thenReturn(trigger);

        // Act
        NotificationTriggerResponse response = notificationService.updateTrigger(triggerId, request);

        // Assert
        assertThat(response).isNotNull();
        verify(triggerRepository).findById(triggerId);
        verify(triggerRepository).save(any(NotificationTrigger.class));
    }

    @Test
    @DisplayName("Should throw exception when trigger not found for update")
    void shouldThrowExceptionWhenTriggerNotFoundForUpdate() {
        // Arrange
        Long triggerId = 999L;
        NotificationTriggerRequest request = new NotificationTriggerRequest();

        when(triggerRepository.findById(triggerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> notificationService.updateTrigger(triggerId, request))
                .isInstanceOf(com.smart.therapy.flow.common.exception.ResourceNotFoundException.class)
                .hasMessageContaining("Notification trigger not found");

        verify(triggerRepository, never()).save(any(NotificationTrigger.class));
    }

    @Test
    @DisplayName("Should delete trigger successfully")
    void shouldDeleteTriggerSuccessfully() {
        // Arrange
        Long triggerId = 1L;
        doNothing().when(triggerRepository).deleteById(triggerId);

        // Act
        notificationService.deleteTrigger(triggerId);

        // Assert
        verify(triggerRepository).deleteById(triggerId);
    }

    @Test
    @DisplayName("Should get all triggers successfully")
    void shouldGetAllTriggersSuccessfully() {
        // Arrange
        when(triggerRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"))).thenReturn(List.of(trigger));

        // Act
        List<NotificationTriggerResponse> triggers = notificationService.getAllTriggers();

        // Assert
        assertThat(triggers).isNotNull();
        assertThat(triggers).hasSize(1);
        verify(triggerRepository).findAll(Sort.by(Sort.Direction.DESC, "createdAt", "id"));
    }

    @Test
    void legacyConditionMapMatchesOnlyMatchingPayload() {
        assertThat(matches("{\"sessionType\":\"assessment\"}", Map.of("sessionType", "assessment"))).isTrue();
        assertThat(matches("{\"sessionType\":\"assessment\"}", Map.of("sessionType", "consultation"))).isFalse();
    }

    @Test
    void invalidConditionsFailClosedWithoutThrowing() {
        for (String rules : List.of("broken json", "42", "null", "{\"operator\":\"equals\"}",
                "{\"field\":\"sessionType\",\"operator\":\"unknown\",\"value\":\"assessment\"}")) {
            assertThat(matches(rules, Map.of("sessionType", "assessment"))).as(rules).isFalse();
        }
    }

    @Test
    void canonicalConditionsAndEmptyDefaultsRemainSupported() {
        assertThat(matches("{}", Map.of())).isTrue();
        assertThat(matches("[]", Map.of())).isTrue();
        assertThat(matches("[{\"field\":\"sessionType\",\"operator\":\"equals\",\"value\":\"assessment\"}]",
                Map.of("sessionType", "assessment"))).isTrue();
    }

    private boolean matches(String rules, Map<String, Object> payload) {
        return org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                notificationService, "matchesConditionRules", rules, payload);
    }

    @Test
    void notificationRunsOnlyAfterCommitInAnIndependentTransaction() {
        var manager = mock(org.springframework.transaction.PlatformTransactionManager.class);
        var status = mock(org.springframework.transaction.TransactionStatus.class);
        when(manager.getTransaction(any())).thenReturn(status);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "transactionTemplate",
                new org.springframework.transaction.support.TransactionTemplate(manager));
        var service = spy(notificationService);
        doThrow(new IllegalStateException("Synthetic notification failure")).when(service).processEvents(any(), any());
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.processEventInNewTransaction("session_scheduled", Map.of("id", 1L));
            verify(service, never()).processEvents(any(), any());
            var callbacks = org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations();
            callbacks.forEach(org.springframework.transaction.support.TransactionSynchronization::afterCommit);
            verify(service).processEvents(eq(List.of("session_scheduled")), eq(Map.of("id", 1L)));
            verify(manager).getTransaction(argThat(definition -> definition.getPropagationBehavior()
                    == org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW));
            verify(manager).rollback(status);
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }

    /**
     * Task and comment payloads carry clientId only to say which chart the work is about. The
     * recipient fallback must never turn that into a client email: internal task chatter reaching
     * the client is a PHI-adjacent leak (observed on prod 2026-09-24 via SparkPost events).
     */
    private com.smart.therapy.flow.client.repository.ClientRepository wireStaffOnlyTaskEvent(String eventType) {
        var clients = mock(com.smart.therapy.flow.client.repository.ClientRepository.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        var sender = mock(com.smart.therapy.flow.common.service.EmailService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "commonEmailService", sender);
        trigger.setEventType(eventType);
        trigger.setRecipientRules(
                "{\"roles\":[\"ADMIN\",\"SUPERVISOR\"],\"assignedTherapist\":true,\"supervisorOfTherapist\":true}");
        trigger.setIsScheduled(false);
        when(triggerRepository.findByEventType(eventType)).thenReturn(List.of(trigger));
        var email = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("email").isActive(true).subject("Task update")
                .bodyTemplate("Internal task update for {{clientMrn}}").build();
        var inApp = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("in_app").isActive(true).subject("Task update").bodyTemplate("Task update").build();
        when(templateRepository.findByEventTypeIgnoreCase(eventType)).thenReturn(List.of(email, inApp));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        return clients;
    }

    @Test
    void taskAssignedNeverTargetsTheClientEvenThoughThePayloadCarriesClientId() {
        var clients = wireStaffOnlyTaskEvent("task_assigned");

        notificationService.processEvent("task_assigned", Map.of(
                "clientId", 7L, "assignedToId", 1L, "id", 55L, "title", "QA Task"));

        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
        verify(notificationRepository, never()).save(argThat(n -> n.getClient() != null));
        verifyNoInteractions(clients);
    }

    @Test
    void taskCommentAddedNeverTargetsTheClientEvenThoughThePayloadCarriesClientId() {
        var clients = wireStaffOnlyTaskEvent("task_comment_added");

        notificationService.processEvent("task_comment_added", Map.of(
                "clientId", 7L, "assignedToId", 1L, "id", 55L, "title", "QA Task"));

        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
        verify(notificationRepository, never()).save(argThat(n -> n.getClient() != null));
        verifyNoInteractions(clients);
    }

    @Test
    void taskTriggerWithoutRecipientRulesStillDoesNotFallBackToTheClient() {
        var clients = wireStaffOnlyTaskEvent("task_assigned");
        trigger.setRecipientRules(null);

        notificationService.processEvent("task_assigned", Map.of(
                "clientId", 7L, "assignedToId", 1L, "id", 55L, "title", "QA Task"));

        // Staff fallback still resolves the assignee; the client fallback stays closed.
        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
        verify(notificationRepository, never()).save(argThat(n -> n.getClient() != null));
        verifyNoInteractions(clients);
    }

    @Test
    void legacySessionTriggerWithoutRecipientRulesStillReachesTheClient() {
        var clients = mock(com.smart.therapy.flow.client.repository.ClientRepository.class);
        var contacts = mock(com.smart.therapy.flow.client.service.ClientContactService.class);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientRepository", clients);
        org.springframework.test.util.ReflectionTestUtils.setField(notificationService, "clientContactService", contacts);
        var client = TestDataFactory.createTestClient();
        client.setId(7L);
        when(clients.findById(7L)).thenReturn(Optional.of(client));
        when(contacts.getPrimaryEmail(7L)).thenReturn(Optional.of(
                com.smart.therapy.flow.client.entity.ClientContact.builder()
                        .contactValue("booking-client@example.com").build()));
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        trigger.setEventType("session_scheduled");
        trigger.setRecipientRules(null);
        trigger.setIsScheduled(false);
        when(triggerRepository.findByEventType("session_scheduled")).thenReturn(List.of(trigger));
        var inApp = com.smart.therapy.flow.notification.entity.NotificationTemplate.builder()
                .type("in_app").isActive(true).subject("Booked").bodyTemplate("Session booked").build();
        when(templateRepository.findByEventTypeIgnoreCase("session_scheduled")).thenReturn(List.of(inApp));

        notificationService.processEvent("session_scheduled", Map.of(
                "clientId", 7L, "therapistId", 1L, "id", 123L));

        verify(notificationRepository).save(argThat(n -> n.getClient() == client));
        verify(notificationRepository).save(argThat(n -> n.getUser() == user));
    }

    @Test
    void rolledBackBookingDoesNotDispatchNotification() {
        var service = spy(notificationService);
        org.springframework.transaction.support.TransactionSynchronizationManager.initSynchronization();
        try {
            service.processEventInNewTransaction("session_scheduled", Map.of("id", 1L));
            org.springframework.transaction.support.TransactionSynchronizationManager.getSynchronizations()
                    .forEach(callback -> callback.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));
            verify(service, never()).processEvents(any(), any());
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager.clearSynchronization();
        }
    }
}
