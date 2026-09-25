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

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeService Unit Tests")
class StripeServiceTest {

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
    @DisplayName("Checkout creation requires organisation context")
    void checkoutRequiresOrganisationContext() {
        TenantContext.clear();

        assertThatThrownBy(() -> stripeService.createCheckoutSession(
                1L, 1L, "100.00", "Therapy", "90837", "online", "2026-01-01", "client@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Organization context is required");
    }

    @Test
    @DisplayName("Checkout creation uses tenant connected account")
    void checkoutUsesTenantConnectedAccount() {
        when(subscriptionFeatureService.isFeatureEnabled(1L, SubscriptionFeatureService.FEATURE_STRIPE_PAYMENTS, null))
                .thenReturn(true);
        when(orgStripeConnectService.requireReadyConnectAccountId(1L)).thenReturn("acct_tenant");
        when(stripePlatformConfigService.requirePlatformSecretKey()).thenReturn("");

        assertThatThrownBy(() -> stripeService.createCheckoutSession(
                1L, 1L, "100.00", "Therapy", "90837", "online", "2026-01-01", "client@example.com"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Stripe API key is not configured");

        org.mockito.Mockito.verify(orgStripeConnectService).requireReadyConnectAccountId(1L);
    }
}
