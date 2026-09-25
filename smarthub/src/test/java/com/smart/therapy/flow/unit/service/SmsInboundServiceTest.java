package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.portal.dto.PortalConsentResponse;
import com.smart.therapy.flow.client.service.ClientContactService;
import com.smart.therapy.flow.client.service.ConsentCommandService;
import com.smart.therapy.flow.common.service.AuditService;
import com.smart.therapy.flow.notification.service.SmsInboundService;
import com.smart.therapy.flow.notification.service.TwilioSmsService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SmsInboundService Unit Tests")
class SmsInboundServiceTest {

    @Mock private TwilioSmsService twilioSmsService;
    @Mock private ClientContactService clientContactService;
    @Mock private ConsentCommandService consentCommandService;
    @Mock private AuditService auditService;

    @InjectMocks
    private SmsInboundService smsInboundService;

    @Test
    void invalidSignatureShouldReturn403() {
        when(twilioSmsService.validateTwilioSignature(anyString(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(false);

        SmsInboundService.InboundResult result = smsInboundService.handleInbound(
                "bad-sig",
                "https://example.com/api/sms/inbound",
                Map.of("Body", "STOP", "From", "+15195551234"),
                "127.0.0.1");

        assertThat(result.statusCode()).isEqualTo(403);
        verify(consentCommandService, never()).recordInboundSmsConsent(anyLong(), anyBoolean(), anyString(), anyString());
    }

    @Test
    void inboundStopShouldWithdrawConsent() {
        Client client = Client.builder().clientId("CL-2026-0042").fullName("Test Client").build();
        client.setId(42L);
        when(twilioSmsService.validateTwilioSignature(anyString(), anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(twilioSmsService.classifyInboundSms("STOP")).thenReturn("opt-out");
        when(clientContactService.findClientsByNormalizedPhone("+15195551234")).thenReturn(List.of(client));
        when(consentCommandService.recordInboundSmsConsent(42L, false, "127.0.0.1", "twilio-inbound-webhook"))
                .thenReturn(PortalConsentResponse.builder().id(7L).clientId(42L).granted(false).build());

        SmsInboundService.InboundResult result = smsInboundService.handleInbound(
                "valid-sig",
                "https://example.com/api/sms/inbound",
                params("STOP", "+15195551234"),
                "127.0.0.1");

        assertThat(result.statusCode()).isEqualTo(200);
        verify(consentCommandService).recordInboundSmsConsent(42L, false, "127.0.0.1", "twilio-inbound-webhook");
        verify(auditService).recordAuditEvent(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void inboundStartShouldGrantConsent() {
        Client client = Client.builder().clientId("CL-2026-0042").fullName("Test Client").build();
        client.setId(42L);
        when(twilioSmsService.validateTwilioSignature(anyString(), anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(twilioSmsService.classifyInboundSms("START")).thenReturn("opt-in");
        when(clientContactService.findClientsByNormalizedPhone("+15195551234")).thenReturn(List.of(client));
        when(consentCommandService.recordInboundSmsConsent(42L, true, "127.0.0.1", "twilio-inbound-webhook"))
                .thenReturn(PortalConsentResponse.builder().id(8L).clientId(42L).granted(true).build());

        SmsInboundService.InboundResult result = smsInboundService.handleInbound(
                "valid-sig",
                "https://example.com/api/sms/inbound",
                params("START", "+15195551234"),
                "127.0.0.1");

        assertThat(result.statusCode()).isEqualTo(200);
        verify(consentCommandService).recordInboundSmsConsent(42L, true, "127.0.0.1", "twilio-inbound-webhook");
    }

    @Test
    void nonKeywordMessageShouldAcknowledgeWithoutConsentChange() {
        when(twilioSmsService.validateTwilioSignature(anyString(), anyString(), org.mockito.ArgumentMatchers.any())).thenReturn(true);
        when(twilioSmsService.classifyInboundSms("hello")).thenReturn(null);

        SmsInboundService.InboundResult result = smsInboundService.handleInbound(
                "valid-sig",
                "https://example.com/api/sms/inbound",
                params("hello", "+15195551234"),
                "127.0.0.1");

        assertThat(result.statusCode()).isEqualTo(200);
        verify(consentCommandService, never()).recordInboundSmsConsent(anyLong(), anyBoolean(), anyString(), anyString());
    }

    private Map<String, String> params(String body, String from) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("Body", body);
        params.put("From", from);
        return params;
    }

}
