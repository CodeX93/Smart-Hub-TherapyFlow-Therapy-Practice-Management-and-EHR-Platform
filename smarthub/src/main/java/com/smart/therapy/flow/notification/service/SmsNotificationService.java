package com.smart.therapy.flow.notification.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.ai.service.ConsentService;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import com.smart.therapy.flow.notification.dto.SmsSendResult;
import com.smart.therapy.flow.notification.enums.TwilioSmsErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Decides whether to send client session SMS and audits every outcome.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationService {

    public static final String SMS_CONSENT_TYPE = "sms_notifications";
    public static final String RESOURCE_TYPE_SMS = "sms_notification";

    private static final Set<String> SESSION_SMS_EVENTS = Set.of(
            NotificationEventCatalog.SESSION_SCHEDULED,
            NotificationEventCatalog.SESSION_SERIES_SCHEDULED,
            NotificationEventCatalog.SESSION_RESCHEDULED,
            NotificationEventCatalog.SESSION_CANCELLED,
            NotificationEventCatalog.SESSION_REMINDER
    );

    private final TwilioSmsService twilioSmsService;
    private final SmsBodyGenerator smsBodyGenerator;
    private final ConsentService consentService;
    private final ClientRepository clientRepository;
    private final ClientContactService clientContactService;
    private final AuditService auditService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public boolean supportsEvent(String eventType) {
        return eventType != null && SESSION_SMS_EVENTS.contains(eventType.toLowerCase());
    }

    public void sendClientSessionSms(Long clientId, String eventType, Boolean isScheduled, Map<String, Object> entityData) {
        if (clientId == null || !supportsEvent(eventType)) {
            return;
        }

        String resourceId = resolveResourceId(entityData, clientId);
        boolean scheduled = Boolean.TRUE.equals(isScheduled);
        log.info("[SMS] Processing client session SMS: clientId={}, eventType={}, resourceId={}, scheduled={}",
                clientId, eventType, resourceId, scheduled);

        try {
            if (!twilioSmsService.isSmsConfigured()) {
                log.info("[SMS] Skipped — Twilio not configured (clientId={}, eventType={})", clientId, eventType);
                auditSms(clientId, resourceId, eventType, "sms_notification_skipped", "skipped",
                        details("reason", "sms_not_configured", "eventType", eventType));
                return;
            }

            String body = smsBodyGenerator.generate(eventType, scheduled, entityData);
            if (!StringUtils.hasText(body)) {
                log.info("[SMS] Skipped — no SMS body template (clientId={}, eventType={})", clientId, eventType);
                auditSms(clientId, resourceId, eventType, "sms_notification_skipped", "skipped",
                        details("reason", "unsupported_event_body", "eventType", eventType));
                return;
            }

            Client client = clientRepository.findById(clientId).orElse(null);
            if (client == null) {
                log.warn("[SMS] Blocked — client not found (clientId={}, eventType={})", clientId, eventType);
                auditSms(clientId, resourceId, eventType, "sms_notification_blocked", "blocked",
                        details("reason", "client_not_found", "eventType", eventType));
                return;
            }

            ConsentService.ConsentCheckResult consent = consentService.checkConsent(clientId, ConsentType.SMS_COMMUNICATION);
            if (!consent.isHasConsent()) {
                log.info("[SMS] Blocked — missing SMS consent (clientId={}, eventType={})", clientId, eventType);
                auditSms(clientId, resourceId, eventType, "sms_notification_blocked", "blocked",
                        details("reason", "missing_or_withdrawn_consent", "consentType", SMS_CONSENT_TYPE, "eventType", eventType));
                return;
            }

            String rawPhone = clientContactService.getPrimaryPhone(clientId)
                    .map(contact -> contact.getContactValue())
                    .orElse(null);
            String phoneE164 = PhoneNormalizationUtil.normalizePhoneE164(rawPhone);
            if (!StringUtils.hasText(phoneE164)) {
                log.info("[SMS] Blocked — missing or invalid phone (clientId={}, eventType={}, rawPhone={})",
                        clientId, eventType, TwilioSmsLogUtil.maskPhone(rawPhone));
                auditSms(clientId, resourceId, eventType, "sms_notification_blocked", "blocked",
                        details("reason", "missing_or_invalid_phone", "eventType", eventType));
                return;
            }

            log.info("[SMS] Dispatching to Twilio (clientId={}, eventType={}, to={}, bodyLength={})",
                    clientId, eventType, TwilioSmsLogUtil.maskPhone(phoneE164), body.length());
            SmsSendResult result = twilioSmsService.sendSms(phoneE164, body);
            if (result.isSuccess()) {
                log.info("[SMS] Sent session SMS (clientId={}, eventType={}, messageSid={})",
                        clientId, eventType, result.getSid());
                auditSms(clientId, resourceId, eventType, "sms_notification_sent", "success",
                        details("messageSid", result.getSid(), "eventType", eventType));
            } else if (TwilioSmsErrorCode.NOT_CONFIGURED.equals(result.getErrorCode())) {
                log.info("[SMS] Skipped at send time — not configured (clientId={}, eventType={})", clientId, eventType);
                auditSms(clientId, resourceId, eventType, "sms_notification_skipped", "skipped",
                        details("reason", "sms_not_configured", "errorCode", result.getErrorCode().name(), "eventType", eventType));
            } else {
                log.warn("[SMS] Failed (clientId={}, eventType={}, errorCode={}, twilioErrorCode={}, error={})",
                        clientId,
                        eventType,
                        result.getErrorCode(),
                        result.getTwilioErrorCode(),
                        result.getError());
                auditSms(clientId, resourceId, eventType, "sms_notification_failed", "failure",
                        details(
                                "error", safe(result.getError()),
                                "errorCode", result.getErrorCode() != null ? result.getErrorCode().name() : "UNKNOWN",
                                "twilioErrorCode", result.getTwilioErrorCode(),
                                "httpStatus", result.getHttpStatusCode(),
                                "eventType", eventType));
            }
        } catch (Exception ex) {
            log.error("[SMS] Unexpected error (clientId={}, eventType={})", clientId, eventType, ex);
            auditSms(clientId, resourceId, eventType, "sms_notification_blocked", "blocked",
                    details("reason", "unexpected_error", "eventType", eventType));
        }
    }

    private String resolveResourceId(Map<String, Object> entityData, Long clientId) {
        if (entityData == null) {
            return String.valueOf(clientId);
        }
        Object sessionId = firstNonNull(entityData.get("sessionId"), entityData.get("id"));
        if (sessionId != null) {
            return String.valueOf(sessionId);
        }
        Object groupId = entityData.get("groupId");
        if (groupId != null) {
            return String.valueOf(groupId);
        }
        return String.valueOf(clientId);
    }

    private void auditSms(Long clientId, String resourceId, String eventType, String action, String result, String details) {
        Client clientRef = clientRepository.findById(clientId).orElse(null);
        auditService.recordAuditEvent(builder -> builder
                .action(action)
                .resourceType(RESOURCE_TYPE_SMS)
                .resourceId(resourceId)
                .client(clientRef)
                .result(result)
                .hipaaRelevant(true)
                .riskLevel("medium")
                .details(details));
    }

    private String details(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i + 1 < keyValues.length; i += 2) {
            Object key = keyValues[i];
            Object value = keyValues[i + 1];
            if (key != null) {
                map.put(String.valueOf(key), value);
            }
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException ex) {
            return "{\"error\":\"audit_details_serialization_failed\"}";
        }
    }

    private String safe(String value) {
        return StringUtils.hasText(value) ? value : "unknown";
    }

    private Object firstNonNull(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }
}
