package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.ai.service.ConsentService;
import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import com.smart.therapy.flow.notification.service.NotificationEventCatalog;
import com.smart.therapy.flow.notification.service.SmsBodyGenerator;
import com.smart.therapy.flow.notification.service.SmsNotificationService;
import com.smart.therapy.flow.notification.service.TwilioSmsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsNotificationService Unit Tests")
class SmsNotificationServiceTest {

    @Mock private TwilioSmsService twilioSmsService;
    @Mock private SmsBodyGenerator smsBodyGenerator;
    @Mock private ConsentService consentService;
    @Mock private ClientRepository clientRepository;
    @Mock private ClientContactService clientContactService;
    @Mock private AuditService auditService;

    @InjectMocks
    private SmsNotificationService smsNotificationService;

    private Map<String, Object> payload;

    @BeforeEach
    void setUp() {
        payload = Map.of(
                "sessionId", 99L,
                "sessionDate", Instant.parse("2026-06-15T18:00:00Z"));
    }

    private Client testClient() {
        Client client = Client.builder()
                .clientId("CL-2026-0001")
                .fullName("Jane Doe")
                .build();
        client.setId(10L);
        return client;
    }

    @Test
    void shouldSupportSessionSmsEvents() {
        assertThat(smsNotificationService.supportsEvent(NotificationEventCatalog.SESSION_SCHEDULED)).isTrue();
        assertThat(smsNotificationService.supportsEvent(NotificationEventCatalog.SESSION_REMINDER)).isTrue();
        assertThat(smsNotificationService.supportsEvent("task_assigned")).isFalse();
    }

    @Test
    void shouldIgnoreUnsupportedEvents() {
        smsNotificationService.sendClientSessionSms(10L, "task_assigned", false, payload);
        verify(auditService, never()).recordAuditEvent(any());
    }

    @Test
    void shouldSkipWhenTwilioUnconfigured() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(false);

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
        assertAuditAction("sms_notification_skipped");
    }

    @Test
    void shouldBlockWhenConsentMissing() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(false).build());

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
        assertAuditAction("sms_notification_blocked");
    }

    @Test
    void shouldBlockWhenConsentWithdrawn() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder()
                        .hasConsent(false)
                        .message("withdrawn")
                        .build());

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
        assertAuditAction("sms_notification_blocked");
    }

    @Test
    void shouldBlockWhenConsentCheckFailsClosed() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder()
                        .hasConsent(false)
                        .error("db error")
                        .build());

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
        assertAuditAction("sms_notification_blocked");
    }

    @Test
    void shouldBlockWhenPhoneInvalid() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(true).build());
        when(clientContactService.getPrimaryPhone(10L)).thenReturn(Optional.of(
                ClientContact.builder().contactType(ContactType.PHONE).contactValue("123").build()));

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService, never()).sendSms(anyString(), anyString());
        assertAuditAction("sms_notification_blocked");
    }

    @Test
    void shouldSendWhenConsentPresentAndPhoneValid() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(true).build());
        when(clientContactService.getPrimaryPhone(10L)).thenReturn(Optional.of(
                ClientContact.builder().contactType(ContactType.PHONE).contactValue("5195551234").build()));
        when(twilioSmsService.sendSms(eq("+15195551234"), eq("Safe body")))
                .thenReturn(SmsSendResult.success("SM123"));

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        verify(twilioSmsService).sendSms("+15195551234", "Safe body");
        assertAuditAction("sms_notification_sent");
    }

    @Test
    void shouldAuditFailureWhenAuthenticationFails() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(true).build());
        when(clientContactService.getPrimaryPhone(10L)).thenReturn(Optional.of(
                ClientContact.builder().contactType(ContactType.PHONE).contactValue("5195551234").build()));
        when(twilioSmsService.sendSms(eq("+15195551234"), eq("Safe body")))
                .thenReturn(SmsSendResult.failure(
                        TwilioSmsErrorCode.AUTHENTICATION_FAILED,
                        "Twilio authentication failed. Verify account SID and auth token."));

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        AuditLog auditLog = captureAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("sms_notification_failed");
        assertThat(auditLog.getDetails()).contains("AUTHENTICATION_FAILED");
    }

    @Test
    void shouldAuditFailureWhenInsufficientCredits() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(true).build());
        when(clientContactService.getPrimaryPhone(10L)).thenReturn(Optional.of(
                ClientContact.builder().contactType(ContactType.PHONE).contactValue("5195551234").build()));
        when(twilioSmsService.sendSms(eq("+15195551234"), eq("Safe body")))
                .thenReturn(SmsSendResult.failure(
                        TwilioSmsErrorCode.INSUFFICIENT_CREDITS,
                        "Twilio account cannot send SMS. Check account balance and SMS permissions."));

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        AuditLog auditLog = captureAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("sms_notification_failed");
        assertThat(auditLog.getDetails()).contains("INSUFFICIENT_CREDITS");
    }

    @Test
    void shouldAuditFailureWhenConfigurationInvalid() {
        when(twilioSmsService.isSmsConfigured()).thenReturn(true);
        when(smsBodyGenerator.generate(anyString(), anyBoolean(), anyMap())).thenReturn("Safe body");
        when(clientRepository.findById(10L)).thenReturn(Optional.of(testClient()));
        when(consentService.checkConsent(10L, ConsentType.SMS_COMMUNICATION))
                .thenReturn(ConsentService.ConsentCheckResult.builder().hasConsent(true).build());
        when(clientContactService.getPrimaryPhone(10L)).thenReturn(Optional.of(
                ClientContact.builder().contactType(ContactType.PHONE).contactValue("5195551234").build()));
        when(twilioSmsService.sendSms(eq("+15195551234"), eq("Safe body")))
                .thenReturn(SmsSendResult.failure(
                        TwilioSmsErrorCode.INVALID_CONFIGURATION,
                        "Twilio account SID is invalid. Expected an Account SID starting with AC."));

        smsNotificationService.sendClientSessionSms(10L, NotificationEventCatalog.SESSION_SCHEDULED, false, payload);

        AuditLog auditLog = captureAuditLog();
        assertThat(auditLog.getAction()).isEqualTo("sms_notification_failed");
        assertThat(auditLog.getDetails()).contains("INVALID_CONFIGURATION");
    }

    private void assertAuditAction(String expectedAction) {
        AuditLog auditLog = captureAuditLog();
        assertThat(auditLog.getAction()).isEqualTo(expectedAction);
        assertThat(auditLog.getResourceType()).isEqualTo(SmsNotificationService.RESOURCE_TYPE_SMS);
    }

    private AuditLog captureAuditLog() {
        ArgumentCaptor<Consumer<AuditLog.AuditLogBuilder>> captor = ArgumentCaptor.forClass(Consumer.class);
        verify(auditService).recordAuditEvent(captor.capture());
        AuditLog.AuditLogBuilder builder = AuditLog.builder();
        captor.getValue().accept(builder);
        return builder.build();
    }
}
