package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.stripe.model.Invoice;
import com.stripe.model.InvoiceCollection;
import com.stripe.param.InvoiceListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeDunningRetryService {

    private final StripePlatformConfigService stripePlatformConfigService;

    /**
     * Attempts payment for the latest open invoice tied to the provider subscription/customer.
     * Returns true when a retry call was executed successfully.
     */
    public boolean retryOpenInvoicePayment(OrgSubscription subscription) {
        if (subscription == null
                || subscription.getProviderCustomerId() == null
                || subscription.getProviderCustomerId().isBlank()
                || subscription.getProviderSubscriptionId() == null
                || subscription.getProviderSubscriptionId().isBlank()) {
            return false;
        }

        try {
            String platformKey = stripePlatformConfigService.requirePlatformSecretKey();
            com.stripe.Stripe.apiKey = platformKey;

            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(subscription.getProviderCustomerId())
                    .setSubscription(subscription.getProviderSubscriptionId())
                    .setStatus(InvoiceListParams.Status.OPEN)
                    .setLimit(1L)
                    .build();
            InvoiceCollection invoices = Invoice.list(params);
            if (invoices == null || invoices.getData() == null || invoices.getData().isEmpty()) {
                return false;
            }
            Invoice invoice = invoices.getData().get(0);
            invoice.pay();
            return true;
        } catch (Exception ex) {
            log.warn("Stripe dunning retry failed for subscriptionId={}: {}", subscription.getId(), ex.getMessage());
            return false;
        }
    }
}
