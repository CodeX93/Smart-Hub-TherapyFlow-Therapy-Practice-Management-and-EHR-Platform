package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.BillingNotificationService;
import com.smart.therapy.flow.subscription.util.SubscriptionPlanPriceResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformSubscriptionInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final BillingNotificationService billingNotificationService;
    private final PlatformAuditService platformAuditService;

    @Transactional
    public Invoice upsertFromStripe(Map<String, Object> stripeInvoiceObject) {
        if (stripeInvoiceObject == null || stripeInvoiceObject.get("id") == null) {
            return null;
        }
        String providerInvoiceId = String.valueOf(stripeInvoiceObject.get("id"));
        Invoice invoice = invoiceRepository.findByProviderInvoiceId(providerInvoiceId);
        if (invoice == null) {
            invoice = resolveByMetadata(stripeInvoiceObject);
        }
        if (invoice == null) {
            invoice = createFromStripe(stripeInvoiceObject, providerInvoiceId);
        } else {
            applyStripeFields(invoice, stripeInvoiceObject, providerInvoiceId);
        }
        if (invoice == null) {
            return null;
        }
        Invoice saved = invoiceRepository.save(invoice);
        platformAuditService.log(null, "SUBSCRIPTION_INVOICE_SYNCED", "Invoice", String.valueOf(saved.getId()),
                "providerInvoiceId=" + providerInvoiceId + ", status=" + saved.getStatus());
        if (saved.getStatus() == InvoiceStatus.PENDING && saved.getOutstandingBalance() != null
                && saved.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0) {
            billingNotificationService.notifySubscriptionInvoiceReady(saved);
        }
        return saved;
    }

    @Transactional
    public Invoice createRenewalInvoice(OrgSubscription subscription) {
        if (subscription == null || subscription.getPlan() == null) {
            return null;
        }
        Instant now = Instant.now();
        BigDecimal amount = subscription.getPriceAtTime() != null
                ? subscription.getPriceAtTime()
                : SubscriptionPlanPriceResolver.resolvePlanPrice(subscription.getPlan(), subscription.getBillingCycleAtTime());
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        Instant periodEnd = resolvePeriodEnd(subscription, now);
        boolean payable = amount.compareTo(BigDecimal.ZERO) > 0;
        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .amount(amount)
                .outstandingBalance(payable ? amount : BigDecimal.ZERO)
                .totalPaid(payable ? BigDecimal.ZERO : amount)
                .refundedAmount(BigDecimal.ZERO)
                .billingPeriodStart(now)
                .billingPeriodEnd(periodEnd)
                .status(payable ? InvoiceStatus.PENDING : InvoiceStatus.PAID)
                .dueDate((payable ? now : periodEnd).atZone(ZoneOffset.UTC).toLocalDate())
                .paidAt(payable ? null : now)
                .build();
        return invoiceRepository.save(invoice);
    }

    @Transactional(readOnly = true)
    public Invoice findFirstOpenInvoice(Long organisationId) {
        if (organisationId == null) {
            return null;
        }
        List<InvoiceStatus> openStatuses = List.of(InvoiceStatus.PENDING, InvoiceStatus.PAST_DUE);
        return invoiceRepository.search(organisationId, null,
                        org.springframework.data.domain.PageRequest.of(0, 50,
                                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")))
                .stream()
                .filter(invoice -> invoice.getStatus() != null && openStatuses.contains(invoice.getStatus()))
                .filter(invoice -> invoice.getOutstandingBalance() == null
                        || invoice.getOutstandingBalance().compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElse(null);
    }

    private Invoice resolveByMetadata(Map<String, Object> stripeInvoiceObject) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = stripeInvoiceObject.get("metadata") instanceof Map
                ? (Map<String, Object>) stripeInvoiceObject.get("metadata") : null;
        if (metadata == null || metadata.get("invoiceId") == null) {
            return null;
        }
        try {
            Long id = Long.valueOf(String.valueOf(metadata.get("invoiceId")));
            return invoiceRepository.findById(id).orElse(null);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Invoice createFromStripe(Map<String, Object> stripeInvoiceObject, String providerInvoiceId) {
        OrgSubscription subscription = resolveSubscription(stripeInvoiceObject);
        if (subscription == null) {
            log.warn("Cannot create local invoice for Stripe invoice {} - subscription not resolved", providerInvoiceId);
            return null;
        }
        Invoice invoice = Invoice.builder()
                .subscription(subscription)
                .amount(BigDecimal.ZERO)
                .outstandingBalance(BigDecimal.ZERO)
                .totalPaid(BigDecimal.ZERO)
                .refundedAmount(BigDecimal.ZERO)
                .billingPeriodStart(Instant.now())
                .billingPeriodEnd(Instant.now())
                .status(InvoiceStatus.PENDING)
                .dueDate(LocalDate.now(ZoneOffset.UTC))
                .build();
        applyStripeFields(invoice, stripeInvoiceObject, providerInvoiceId);
        return invoice;
    }

    private OrgSubscription resolveSubscription(Map<String, Object> stripeInvoiceObject) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = stripeInvoiceObject.get("metadata") instanceof Map
                ? (Map<String, Object>) stripeInvoiceObject.get("metadata") : null;
        if (metadata != null && metadata.get("subscriptionId") != null) {
            try {
                Long subscriptionId = Long.valueOf(String.valueOf(metadata.get("subscriptionId")));
                return orgSubscriptionRepository.findById(subscriptionId).orElse(null);
            } catch (NumberFormatException ignored) {
                // continue
            }
        }
        if (metadata != null && metadata.get("organisationId") != null) {
            try {
                Long organisationId = Long.valueOf(String.valueOf(metadata.get("organisationId")));
                return orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).orElse(null);
            } catch (NumberFormatException ignored) {
                // continue
            }
        }
        String providerSubId = stripeInvoiceObject.get("subscription") != null
                ? String.valueOf(stripeInvoiceObject.get("subscription")) : null;
        if (StringUtils.hasText(providerSubId) && !"null".equalsIgnoreCase(providerSubId)) {
            return orgSubscriptionRepository.findByProviderSubscriptionIdAndEndAtIsNull(providerSubId).orElse(null);
        }
        String providerCustomerId = stripeInvoiceObject.get("customer") != null
                ? String.valueOf(stripeInvoiceObject.get("customer")) : null;
        if (StringUtils.hasText(providerCustomerId) && !"null".equalsIgnoreCase(providerCustomerId)) {
            return orgSubscriptionRepository.findByProviderCustomerIdAndEndAtIsNull(providerCustomerId).orElse(null);
        }
        return null;
    }

    private void applyStripeFields(Invoice invoice, Map<String, Object> object, String providerInvoiceId) {
        invoice.setProviderInvoiceId(providerInvoiceId);
        BigDecimal amountDue = centsToUsd(object.get("amount_due"));
        BigDecimal amountPaid = centsToUsd(object.get("amount_paid"));
        if (amountDue != null) {
            invoice.setAmount(amountDue);
            invoice.setOutstandingBalance(amountDue);
        }
        if (amountPaid != null && amountPaid.compareTo(BigDecimal.ZERO) > 0) {
            invoice.setTotalPaid(amountPaid);
            BigDecimal outstanding = invoice.getAmount().subtract(amountPaid);
            invoice.setOutstandingBalance(outstanding.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));
        }
        Instant periodStart = epochSeconds(object.get("period_start"));
        Instant periodEnd = epochSeconds(object.get("period_end"));
        if (periodStart != null) {
            invoice.setBillingPeriodStart(periodStart);
        }
        if (periodEnd != null) {
            invoice.setBillingPeriodEnd(periodEnd);
        }
        if (object.get("due_date") instanceof Number dueDateEpoch) {
            invoice.setDueDate(Instant.ofEpochSecond(dueDateEpoch.longValue()).atZone(ZoneOffset.UTC).toLocalDate());
        }
        String stripeStatus = object.get("status") instanceof String s ? s : null;
        if ("paid".equalsIgnoreCase(stripeStatus)) {
            invoice.setStatus(InvoiceStatus.PAID);
            invoice.setPaidAt(Instant.now());
            invoice.setOutstandingBalance(BigDecimal.ZERO);
        } else if ("open".equalsIgnoreCase(stripeStatus) || "draft".equalsIgnoreCase(stripeStatus)) {
            invoice.setStatus(InvoiceStatus.PENDING);
        } else if ("uncollectible".equalsIgnoreCase(stripeStatus) || "past_due".equalsIgnoreCase(stripeStatus)) {
            invoice.setStatus(InvoiceStatus.PAST_DUE);
        }
        if (object.get("charge") != null) {
            invoice.setProviderChargeId(String.valueOf(object.get("charge")));
        }
        if (object.get("payment_intent") != null) {
            invoice.setProviderPaymentIntentId(String.valueOf(object.get("payment_intent")));
        }
    }

    private static Instant resolvePeriodEnd(OrgSubscription subscription, Instant start) {
        if (subscription.getProviderCurrentPeriodEnd() != null) {
            return subscription.getProviderCurrentPeriodEnd();
        }
        String cycle = subscription.getBillingCycleAtTime();
        if (cycle == null) {
            return start.plusSeconds(30L * 24 * 3600);
        }
        return switch (cycle.toLowerCase()) {
            case "yearly", "annual" -> start.plusSeconds(365L * 24 * 3600);
            case "monthly" -> start.plusSeconds(30L * 24 * 3600);
            default -> start.plusSeconds(30L * 24 * 3600);
        };
    }

    private static BigDecimal centsToUsd(Object centsValue) {
        if (!(centsValue instanceof Number number)) {
            return null;
        }
        return BigDecimal.valueOf(number.longValue()).movePointLeft(2);
    }

    private static Instant epochSeconds(Object value) {
        if (!(value instanceof Number number)) {
            return null;
        }
        long epoch = number.longValue();
        return epoch > 0 ? Instant.ofEpochSecond(epoch) : null;
    }
}
