package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.payment.service.StripePlatformConfigService;
import com.smart.therapy.flow.payment.service.StripePlatformSubscriptionService;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import com.stripe.exception.StripeException;
import com.stripe.model.Invoice;
import com.stripe.net.RequestOptions;
import com.stripe.param.InvoiceListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One-time migration helper to move organisations from manual_or_unconfigured to provider_managed.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionBackfillService {

    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final StripePlatformSubscriptionService stripePlatformSubscriptionService;
    private final PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;
    private final StripePlatformConfigService stripePlatformConfigService;
    private final PlatformAuditService platformAuditService;

    @Transactional(readOnly = true)
    public List<Long> listManualOrUnconfiguredOrganisationIds() {
        return orgSubscriptionRepository.findAllCurrentWithOrganisationAndPlan().stream()
                .filter(sub -> !SubscriptionPlanPriceResolver.isProviderManaged(
                        sub.getPlan(), sub.getProviderCustomerId(), sub.getProviderSubscriptionId()))
                .filter(sub -> sub.getOrganisation() != null && sub.getOrganisation().getId() != null)
                .map(sub -> sub.getOrganisation().getId())
                .distinct()
                .toList();
    }

    @Transactional
    public BackfillResult backfillOrganisation(Long organisationId, Long actorAuthId) {
        OrgSubscription subscription = orgSubscriptionRepository.findCurrentByOrganisationId(organisationId)
                .orElse(null);
        if (subscription == null) {
            return BackfillResult.organisationNotFound();
        }
        if (SubscriptionPlanPriceResolver.isProviderManaged(
                subscription.getPlan(), subscription.getProviderCustomerId(), subscription.getProviderSubscriptionId())) {
            int synced = syncStripeInvoices(subscription);
            return BackfillResult.alreadyProvisioned(synced);
        }

        OrgSubscription provisioned = stripePlatformSubscriptionService.provisionStripeSubscription(organisationId, actorAuthId);
        int synced = syncStripeInvoices(provisioned);
        platformAuditService.log(actorAuthId, "SUBSCRIPTION_BACKFILL_COMPLETED", "Organisation",
                String.valueOf(organisationId),
                "providerSubscriptionId=" + provisioned.getProviderSubscriptionId() + ", invoicesSynced=" + synced);
        return BackfillResult.success(provisioned.getProviderSubscriptionId(), synced);
    }

    private int syncStripeInvoices(OrgSubscription subscription) {
        if (subscription == null || !StringUtils.hasText(subscription.getProviderCustomerId())) {
            return 0;
        }
        try {
            RequestOptions requestOptions = RequestOptions.builder()
                    .setApiKey(stripePlatformConfigService.requirePlatformSecretKey())
                    .build();
            InvoiceListParams params = InvoiceListParams.builder()
                    .setCustomer(subscription.getProviderCustomerId())
                    .setLimit(20L)
                    .build();
            List<Invoice> invoices = Invoice.list(params, requestOptions).getData();
            int synced = 0;
            for (Invoice stripeInvoice : invoices) {
                if (stripeInvoice == null || stripeInvoice.getId() == null) {
                    continue;
                }
                Map<String, Object> object = new HashMap<>();
                object.put("id", stripeInvoice.getId());
                object.put("customer", stripeInvoice.getCustomer());
                object.put("subscription", stripeInvoice.getSubscription());
                object.put("amount_due", stripeInvoice.getAmountDue());
                object.put("amount_paid", stripeInvoice.getAmountPaid());
                object.put("period_start", stripeInvoice.getPeriodStart());
                object.put("period_end", stripeInvoice.getPeriodEnd());
                object.put("due_date", stripeInvoice.getDueDate());
                object.put("status", stripeInvoice.getStatus());
                object.put("charge", stripeInvoice.getCharge());
                object.put("payment_intent", stripeInvoice.getPaymentIntent());
                if (stripeInvoice.getMetadata() != null) {
                    object.put("metadata", stripeInvoice.getMetadata());
                }
                platformSubscriptionInvoiceService.upsertFromStripe(object);
                synced++;
            }
            return synced;
        } catch (StripeException ex) {
            log.warn("Stripe invoice sync skipped for subscription {}: {}", subscription.getId(), ex.getMessage());
            return 0;
        }
    }

    public record BackfillResult(boolean success, boolean notFound, boolean alreadyProvisioned,
                                 String providerSubscriptionId, int invoicesSynced) {
        public static BackfillResult success(String providerSubscriptionId, int invoicesSynced) {
            return new BackfillResult(true, false, false, providerSubscriptionId, invoicesSynced);
        }

        public static BackfillResult organisationNotFound() {
            return new BackfillResult(false, true, false, null, 0);
        }

        public static BackfillResult alreadyProvisioned(int invoicesSynced) {
            return new BackfillResult(true, false, true, null, invoicesSynced);
        }
    }
}
