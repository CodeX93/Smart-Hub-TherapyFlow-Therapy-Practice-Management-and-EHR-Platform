package com.smart.therapy.flow.unit.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.payment.service.OrgStripeConnectService;
import com.smart.therapy.flow.payment.service.StripePlatformConfigService;
import com.smart.therapy.flow.payment.service.StripeService;
import com.smart.therapy.flow.payment.service.StripeSubscriptionWebhookService;
import com.smart.therapy.flow.payment.service.StripeTenantConfigService;
import com.smart.therapy.flow.payment.service.StripeWebhookApplicationService;
import com.smart.therapy.flow.payment.service.StripeWebhookEventService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookApplicationService Unit Tests")
class StripeWebhookApplicationServiceTest {

    @Mock private StripeSubscriptionWebhookService stripeSubscriptionWebhookService;
    @Mock private StripeWebhookEventService stripeWebhookEventService;
    @Mock private StripePlatformConfigService stripePlatformConfigService;
    @Mock private StripeTenantConfigService stripeTenantConfigService;
    @Mock private OrgStripeConnectService orgStripeConnectService;
    @Mock private StripeService stripeService;
    @Mock private PlatformAuditService platformAuditService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks
    private StripeWebhookApplicationService webhookApplicationService;

    @Test
    @DisplayName("Missing webhook signature is rejected")
    void missingWebhookSignatureRejected() {
        when(stripePlatformConfigService.requireConnectWebhookSecret()).thenReturn("whsec_test");

        ResponseEntity<Object> response = webhookApplicationService.handleConnectWebhook("{}", "");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("Missing Stripe-Signature header");
    }

    @Test
    @DisplayName("Invalid webhook signature is rejected")
    void invalidWebhookSignatureRejected() {
        when(stripePlatformConfigService.requireConnectWebhookSecret()).thenReturn("whsec_test");

        ResponseEntity<Object> response = webhookApplicationService.handleConnectWebhook(
                "{\"id\":\"evt_test\",\"type\":\"checkout.session.completed\"}",
                "t=123,v1=invalid_signature");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(((Map<?, ?>) response.getBody()).get("error")).isEqualTo("Invalid Stripe signature");
    }
}
