package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.billing.service.PlatformSubscriptionInvoiceService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.payment.service.StripeSubscriptionWebhookService;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeSubscriptionWebhookService tests")
class StripeSubscriptionWebhookServiceTest {

    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private SubscriptionLifecycleService lifecycleService;
    @Mock
    private PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;
    @Mock
    private StripePlatformSubscriptionService stripePlatformSubscriptionService;

    @InjectMocks
    private StripeSubscriptionWebhookService webhookService;

    @Test
    @DisplayName("Handles checkout.session.completed for subscription mode")
    void shouldHandleCheckoutCompleted() {
        boolean handled = webhookService.handleLifecycleEvent("checkout.session.completed", Map.of(
                "object", Map.of(
                        "mode", "subscription",
                        "customer", "cus_123",
                        "subscription", "sub_123",
                        "metadata", Map.of("organisationId", "99")
                )
        ));

        assertThat(handled).isTrue();
        verify(stripePlatformSubscriptionService).persistProviderIdsFromCheckout(99L, "cus_123", "sub_123");
        verify(stripePlatformSubscriptionService).syncStripeSubscriptionPeriod(eq(null), eq("sub_123"));
    }

    @Test
    @DisplayName("Ignores checkout.session.completed for payment mode (client invoices)")
    void shouldIgnoreCheckoutCompletedForPaymentMode() {
        boolean handled = webhookService.handleLifecycleEvent("checkout.session.completed", Map.of(
                "object", Map.of(
                        "mode", "payment",
                        "customer", "cus_123",
                        "metadata", Map.of(
                                "invoiceId", "10",
                                "clientId", "20",
                                "organisationId", "99",
                                "portalPayment", "true"
                        )
                )
        ));

        assertThat(handled).isTrue();
        verifyNoInteractions(stripePlatformSubscriptionService);
    }

    @Test
    @DisplayName("Upserts invoice on invoice.created")
    void shouldUpsertInvoiceOnCreated() {
        boolean handled = webhookService.handleLifecycleEvent("invoice.created", Map.of(
                "object", Map.of("id", "in_123", "subscription", "sub_123")
        ));

        assertThat(handled).isTrue();
        verify(platformSubscriptionInvoiceService).upsertFromStripe(any());
    }

    @Test
    @DisplayName("Triggers lifecycle on invoice.payment_succeeded")
    void shouldTriggerLifecycleOnPaymentSucceeded() {
        OrgSubscription subscription = new OrgSubscription();
        Organisation organisation = new Organisation();
        organisation.setId(99L);
        subscription.setOrganisation(organisation);
        when(orgSubscriptionRepository.findByProviderSubscriptionIdAndEndAtIsNull("sub_123"))
                .thenReturn(Optional.of(subscription));

        boolean handled = webhookService.handleLifecycleEvent("invoice.payment_succeeded", Map.of(
                "object", Map.of(
                        "id", "in_123",
                        "subscription", "sub_123",
                        "amount_paid", 9900
                )
        ), "evt_123");

        assertThat(handled).isTrue();
        verify(platformSubscriptionInvoiceService).upsertFromStripe(any());
        verify(lifecycleService).onPaymentSucceeded(99L, "stripe:invoice.payment_succeeded");
    }
}
