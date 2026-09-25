package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.dto.PortalConsentResponse;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.client.service.ConsentCommandService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class SmsInboundService {

    public static final String RESOURCE_TYPE_PATIENT_CONSENT = "patient_consent";
    public static final String EMPTY_TWIML = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><Response></Response>";

    private final TwilioSmsService twilioSmsService;
    private final ClientContactService clientContactService;
    private final ConsentCommandService consentCommandService;
    private final AuditService auditService;

    public record InboundResult(int statusCode, String body) {
        public static InboundResult ok() {
            return new InboundResult(200, EMPTY_TWIML);
        }

        public static InboundResult forbidden() {
            return new InboundResult(403, EMPTY_TWIML);
        }
    }

    @Transactional
    public InboundResult handleInbound(String signature, String requestUrl, Map<String, String> params, String ipAddress) {
        if (!twilioSmsService.validateTwilioSignature(signature, requestUrl, params)) {
            return InboundResult.forbidden();
        }

        try {
            String body = params.get("Body");
            String intent = twilioSmsService.classifyInboundSms(body);
            if (intent == null) {
                return InboundResult.ok();
            }

            String fromE164 = PhoneNormalizationUtil.normalizePhoneE164(params.get("From"));
            if (!StringUtils.hasText(fromE164)) {
                return InboundResult.ok();
            }

            List<Client> clients = clientContactService.findClientsByNormalizedPhone(fromE164);
            if (clients.isEmpty()) {
                return InboundResult.ok();
            }

            boolean grant = "opt-in".equals(intent);
            for (Client client : clients) {
                PortalConsentResponse response = consentCommandService.recordInboundSmsConsent(
                        client.getId(),
                        grant,
                        ipAddress,
                        "twilio-inbound-webhook");
                auditConsentChange(client, grant, response);
            }
        } catch (Exception ex) {
            log.error("Inbound SMS processing failed", ex);
        }

        return InboundResult.ok();
    }

    private void auditConsentChange(Client client, boolean granted, PortalConsentResponse response) {
        auditService.recordAuditEvent(builder -> builder
                .action(granted ? "consent_granted" : "consent_withdrawn")
                .resourceType(RESOURCE_TYPE_PATIENT_CONSENT)
                .resourceId(response != null && response.getId() != null ? String.valueOf(response.getId()) : null)
                .client(client)
                .result("success")
                .hipaaRelevant(true)
                .riskLevel("high")
                .details(String.format(
                        "{\"consentType\":\"%s\",\"source\":\"twilio_inbound\",\"clientId\":%d}",
                        SmsNotificationService.SMS_CONSENT_TYPE,
                        client.getId())));
    }
}
