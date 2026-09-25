package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.billing.service.PlatformSubscriptionInvoiceService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.SubscriptionPlan;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.BillingNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformSubscriptionInvoiceService tests")
class PlatformSubscriptionInvoiceServiceTest {

    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private OrgSubscriptionRepository orgSubscriptionRepository;
    @Mock
    private BillingNotificationService billingNotificationService;
    @Mock
    private PlatformAuditService platformAuditService;

    @InjectMocks
    private PlatformSubscriptionInvoiceService service;

    @Test
    @DisplayName("Upserts Stripe invoice by provider id")
    void shouldUpsertStripeInvoice() {
        OrgSubscription subscription = subscription(99L);
        when(invoiceRepository.findByProviderInvoiceId("in_123")).thenReturn(null);
        when(orgSubscriptionRepository.findByProviderSubscriptionIdAndEndAtIsNull("sub_123"))
                .thenReturn(Optional.of(subscription));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice invoice = inv.getArgument(0);
            invoice.setId(55L);
            return invoice;
        });

        Invoice saved = service.upsertFromStripe(Map.of(
                "id", "in_123",
                "subscription", "sub_123",
                "amount_due", 9900,
                "amount_paid", 0,
                "status", "open",
                "period_start", 1_700_000_000L,
                "period_end", 1_702_592_000L
        ));

        assertThat(saved).isNotNull();
        assertThat(saved.getProviderInvoiceId()).isEqualTo("in_123");
        assertThat(saved.getAmount()).isEqualByComparingTo("99.00");
        assertThat(saved.getStatus()).isEqualTo(InvoiceStatus.PENDING);
        verify(billingNotificationService).notifySubscriptionInvoiceReady(saved);
    }

    @Test
    @DisplayName("Creates manual renewal invoice for subscription")
    void shouldCreateManualRenewalInvoice() {
        OrgSubscription subscription = subscription(99L);
        subscription.setPriceAtTime(new BigDecimal("149.00"));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));

        Invoice invoice = service.createRenewalInvoice(subscription);

        ArgumentCaptor<Invoice> captor = ArgumentCaptor.forClass(Invoice.class);
        verify(invoiceRepository).save(captor.capture());
        assertThat(captor.getValue().getAmount()).isEqualByComparingTo("149.00");
        assertThat(captor.getValue().getStatus()).isEqualTo(InvoiceStatus.PENDING);
        assertThat(invoice.getOutstandingBalance()).isEqualByComparingTo("149.00");
    }

    private static OrgSubscription subscription(Long orgId) {
        Organisation organisation = new Organisation();
        organisation.setId(orgId);

        SubscriptionPlan plan = new SubscriptionPlan();
        plan.setCode("PRO");
        plan.setBasePrice(new BigDecimal("99.00"));

        OrgSubscription subscription = new OrgSubscription();
        subscription.setId(10L);
        subscription.setOrganisation(organisation);
        subscription.setPlan(plan);
        subscription.setBillingCycleAtTime("monthly");
        subscription.setPriceAtTime(new BigDecimal("99.00"));
        return subscription;
    }
}
