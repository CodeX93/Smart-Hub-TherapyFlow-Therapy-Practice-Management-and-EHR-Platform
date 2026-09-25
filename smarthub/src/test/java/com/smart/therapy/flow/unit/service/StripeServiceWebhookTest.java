package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.billing.repository.PaymentRepository;
import com.smart.therapy.flow.billing.repository.PaymentTransactionRepository;
import com.smart.therapy.flow.billing.repository.SessionBillingRepository;
import com.smart.therapy.flow.billing.service.BillingService;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.payment.service.OrgStripeConnectService;
import com.smart.therapy.flow.payment.service.StripePlatformConfigService;
import com.smart.therapy.flow.payment.service.StripeService;
import com.smart.therapy.flow.payment.service.StripeTenantConfigService;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeService Webhook Unit Tests")
class StripeServiceWebhookTest {

    @Mock private SessionBillingRepository billingRepository;
    @Mock private AuditLogRepository auditLogRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private PaymentTransactionRepository paymentTransactionRepository;
    @Mock private StripePlatformConfigService stripePlatformConfigService;
    @Mock private StripeTenantConfigService stripeTenantConfigService;
    @Mock private OrgStripeConnectService orgStripeConnectService;
    @Mock private SubscriptionFeatureService subscriptionFeatureService;
    @Mock private BillingService billingService;

    @InjectMocks
    private StripeService stripeService;

    @BeforeEach
    void setUp() {
        TenantContext.setOrganisationId(1L);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @DisplayName("Success redirect event type does not mark invoice paid")
    void successRedirectEventTypeDoesNotMarkPaid() {
        stripeService.handleConnectedAccountWebhookEvent(Map.of(), "payment_intent.succeeded", "acct_1");

        verify(billingService, never()).applyStripePortalPayment(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Verified checkout.session.completed webhook marks paid")
    void verifiedCheckoutCompletedMarksPaid() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "10");
        metadata.put("clientId", "42");
        metadata.put("organisationId", "1");
        metadata.put("connectedAccountId", "acct_tenant");

        Map<String, Object> sessionObject = new HashMap<>();
        sessionObject.put("metadata", metadata);
        sessionObject.put("amount_total", 10000L);
        sessionObject.put("payment_intent", "pi_test");
        sessionObject.put("id", "cs_test");

        Map<String, Object> eventData = Map.of("object", sessionObject);

        stripeService.handleConnectedAccountWebhookEvent(eventData, "checkout.session.completed", "acct_tenant");

        verify(billingService).applyStripePortalPayment(
                eq(10L),
                eq(42L),
                eq(1L),
                eq(new BigDecimal("100.0")),
                eq("pi_test"),
                eq("cs_test"),
                eq("acct_tenant"),
                eq("acct_tenant"));
    }

    @Test
    @DisplayName("Webhook organisation mismatch is rejected")
    void webhookOrganisationMismatchRejected() {
        TenantContext.setOrganisationId(99L);

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("invoiceId", "10");
        metadata.put("clientId", "42");
        metadata.put("organisationId", "1");

        Map<String, Object> sessionObject = new HashMap<>();
        sessionObject.put("metadata", metadata);
        sessionObject.put("amount_total", 10000L);

        Map<String, Object> eventData = Map.of("object", sessionObject);

        assertThatThrownBy(() -> stripeService.handleConnectedAccountWebhookEvent(
                eventData, "checkout.session.completed", "acct_tenant"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("organisation metadata mismatch");

        verify(billingService, never()).applyStripePortalPayment(
                any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Connect webhook ignores checkout without client session billing metadata")
    void connectWebhookIgnoresSubscriptionCheckoutMetadata() {
        Map<String, Object> sessionObject = new HashMap<>();
        sessionObject.put("metadata", Map.of(
                "organisationId", "1",
                "subscriptionId", "10"
        ));
        sessionObject.put("mode", "subscription");
        sessionObject.put("id", "cs_sub_test");

        stripeService.handleConnectedAccountWebhookEvent(
                Map.of("object", sessionObject),
                "checkout.session.completed",
                "acct_tenant");

        verify(billingService, never()).applyStripePortalPayment(
                any(), any(), any(), any(), any(), any(), any(), any());
    }
}
