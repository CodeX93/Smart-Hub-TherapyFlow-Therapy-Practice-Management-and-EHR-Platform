package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.billing.service.PlatformSubscriptionInvoiceService;
import com.smart.therapy.flow.subscription.entity.OrgSubscription;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.subscription.repository.OrgSubscriptionRepository;
import com.smart.therapy.flow.subscription.service.SubscriptionLifecycleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Map;
import java.util.Optional;
import java.math.BigDecimal;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class StripeSubscriptionWebhookService {

    private final OrgSubscriptionRepository orgSubscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final SubscriptionLifecycleService lifecycleService;
    private final PlatformSubscriptionInvoiceService platformSubscriptionInvoiceService;
    private final StripePlatformSubscriptionService stripePlatformSubscriptionService;
    @Value("${subscription.lifecycle.webhook.lock-retries:4}")
    private int webhookLockRetries;
    @Value("${subscription.lifecycle.webhook.lock-backoff-ms:120}")
    private long webhookLockBackoffMs;

    /**
     * Handles subscription lifecycle webhooks.
     * Returns true when the event type was recognized as a subscription-billing lifecycle event.
     */
    public boolean handleLifecycleEvent(String eventType, Map<String, Object> eventData) {
        return handleLifecycleEvent(eventType, eventData, null);
    }

    public boolean handleLifecycleEvent(String eventType, Map<String, Object> eventData, String providerEventId) {
        if (eventType == null || eventData == null) {
            return false;
        }

        boolean recognized = eventType.startsWith("invoice.")
                || eventType.startsWith("customer.subscription.")
                || "checkout.session.completed".equals(eventType);

        if (!recognized) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> object = (Map<String, Object>) eventData.get("object");
        if (object == null) {
            log.warn("Stripe lifecycle webhook missing data.object for type={}", eventType);
            return true;
        }

        switch (eventType) {
            case "checkout.session.completed" -> handleCheckoutCompleted(object);
            case "invoice.created", "invoice.finalized" -> platformSubscriptionInvoiceService.upsertFromStripe(object);
            case "invoice.payment_failed" -> {
                platformSubscriptionInvoiceService.upsertFromStripe(object);
                markInvoicePaymentFailed(object);
                Long organisationId = resolveOrganisationId(object);
                if (organisationId != null) {
                    invokeLifecycleWithRetry(
                            () -> lifecycleService.onPaymentFailed(organisationId, "stripe:invoice.payment_failed"),
                            "invoice.payment_failed",
                            organisationId,
                            providerEventId
                    );
                }
            }
            case "invoice.payment_succeeded" -> {
                platformSubscriptionInvoiceService.upsertFromStripe(object);
                markInvoicePaid(object);
                Long organisationId = resolveOrganisationId(object);
                if (organisationId != null) {
                    invokeLifecycleWithRetry(
                            () -> lifecycleService.onPaymentSucceeded(organisationId, "stripe:invoice.payment_succeeded"),
                            "invoice.payment_succeeded",
                            organisationId,
                            providerEventId
                    );
                }
            }
            case "invoice.refunded" -> markInvoiceRefunded(object);
            case "customer.subscription.created" -> {
                Long organisationId = resolveOrganisationId(object);
                persistProviderSubscription(object, organisationId);
            }
            case "customer.subscription.deleted" -> {
                Long organisationId = resolveOrganisationId(object);
                if (organisationId != null) {
                    invokeLifecycleWithRetry(
                            () -> lifecycleService.onProviderSubscriptionCancelled(organisationId, "stripe:customer.subscription.deleted"),
                            "customer.subscription.deleted",
                            organisationId,
                            providerEventId
                    );
                }
            }
            case "customer.subscription.updated" -> {
                Long organisationId = resolveOrganisationId(object);
                if (organisationId != null) {
                    syncProviderSubscriptionPeriod(object, organisationId);
                    handleSubscriptionUpdated(organisationId, object, providerEventId);
                }
            }
            default -> log.debug("Stripe lifecycle event ignored: {}", eventType);
        }
        return true;
    }

    private void handleCheckoutCompleted(Map<String, Object> object) {
        String mode = object.get("mode") instanceof String ? (String) object.get("mode") : null;
        if (!"subscription".equalsIgnoreCase(mode)) {
            return;
        }
        Long organisationId = resolveOrganisationIdFromMetadata(object);
        String customerId = object.get("customer") != null ? String.valueOf(object.get("customer")) : null;
        String subscriptionId = object.get("subscription") != null ? String.valueOf(object.get("subscription")) : null;
        if (organisationId != null && StringUtils.hasText(customerId) && StringUtils.hasText(subscriptionId)
                && !"null".equalsIgnoreCase(customerId) && !"null".equalsIgnoreCase(subscriptionId)) {
            stripePlatformSubscriptionService.persistProviderIdsFromCheckout(organisationId, customerId, subscriptionId);
            stripePlatformSubscriptionService.syncStripeSubscriptionPeriod(null, subscriptionId);
        }
    }

    private void persistProviderSubscription(Map<String, Object> object, Long organisationId) {
        if (organisationId == null) {
            return;
        }
        String customerId = object.get("customer") != null ? String.valueOf(object.get("customer")) : null;
        String subscriptionId = object.get("id") != null ? String.valueOf(object.get("id")) : null;
        if (StringUtils.hasText(customerId) && StringUtils.hasText(subscriptionId)
                && !"null".equalsIgnoreCase(customerId) && !"null".equalsIgnoreCase(subscriptionId)) {
            stripePlatformSubscriptionService.persistProviderIdsFromCheckout(organisationId, customerId, subscriptionId);
            stripePlatformSubscriptionService.syncStripeSubscriptionPeriod(null, subscriptionId);
        }
    }

    private void syncProviderSubscriptionPeriod(Map<String, Object> object, Long organisationId) {
        String subscriptionId = object.get("id") != null ? String.valueOf(object.get("id")) : null;
        if (!StringUtils.hasText(subscriptionId) || "null".equalsIgnoreCase(subscriptionId)) {
            return;
        }
        orgSubscriptionRepository.findCurrentByOrganisationId(organisationId).ifPresent(sub -> {
            stripePlatformSubscriptionService.syncStripeSubscriptionPeriod(sub.getId(), subscriptionId);
        });
    }

    private Long resolveOrganisationIdFromMetadata(Map<String, Object> object) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = object.get("metadata") instanceof Map ? (Map<String, Object>) object.get("metadata") : null;
        if (metadata != null && metadata.get("organisationId") != null) {
            try {
                return Long.valueOf(String.valueOf(metadata.get("organisationId")));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private void handleSubscriptionUpdated(Long organisationId, Map<String, Object> object, String providerEventId) {
        String status = object.get("status") instanceof String ? (String) object.get("status") : null;
        if (status == null) {
            return;
        }
        switch (status) {
            case "active", "trialing" -> invokeLifecycleWithRetry(
                    () -> lifecycleService.onPaymentSucceeded(organisationId, "stripe:customer.subscription.updated:" + status),
                    "customer.subscription.updated:" + status,
                    organisationId,
                    providerEventId
            );
            case "past_due", "unpaid" -> invokeLifecycleWithRetry(
                    () -> lifecycleService.onPaymentFailed(organisationId, "stripe:customer.subscription.updated:" + status),
                    "customer.subscription.updated:" + status,
                    organisationId,
                    providerEventId
            );
            case "canceled" -> invokeLifecycleWithRetry(
                    () -> lifecycleService.onProviderSubscriptionCancelled(organisationId, "stripe:customer.subscription.updated:canceled"),
                    "customer.subscription.updated:canceled",
                    organisationId,
                    providerEventId
            );
            default -> log.debug("Unhandled Stripe subscription status={}", status);
        }
    }

    private void invokeLifecycleWithRetry(Runnable action,
                                          String lifecycleEvent,
                                          Long organisationId,
                                          String providerEventId) {
        int attempts = Math.max(1, webhookLockRetries);
        long baseDelay = Math.max(25L, webhookLockBackoffMs);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            try {
                action.run();
                return;
            } catch (PessimisticLockingFailureException ex) {
                if (attempt >= attempts) {
                    throw ex;
                }
                long jitter = ThreadLocalRandom.current().nextLong(Math.max(20L, baseDelay));
                long delayMs = Math.min(2_000L, baseDelay * (1L << (attempt - 1)) + jitter);
                log.warn("Lifecycle lock contention event={}, orgId={}, providerEventId={}, attempt={}/{}, retryInMs={}",
                        lifecycleEvent, organisationId, providerEventId, attempt, attempts, delayMs);
                sleep(delayMs);
            }
        }
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    private Long resolveOrganisationId(Map<String, Object> object) {
        Long fromMetadata = resolveOrganisationIdFromMetadata(object);
        if (fromMetadata != null) {
            return fromMetadata;
        }

        String providerSubId = object.get("subscription") != null ? String.valueOf(object.get("subscription")) : null;
        if (providerSubId == null || "null".equalsIgnoreCase(providerSubId)) {
            providerSubId = object.get("id") != null ? String.valueOf(object.get("id")) : null;
        }
        if (providerSubId != null && !"null".equalsIgnoreCase(providerSubId)) {
            Optional<OrgSubscription> bySub = orgSubscriptionRepository.findByProviderSubscriptionIdAndEndAtIsNull(providerSubId);
            if (bySub.isPresent()) {
                return bySub.get().getOrganisation().getId();
            }
        }

        String providerCustomerId = object.get("customer") != null ? String.valueOf(object.get("customer")) : null;
        if (providerCustomerId != null && !"null".equalsIgnoreCase(providerCustomerId)) {
            Optional<OrgSubscription> byCustomer = orgSubscriptionRepository.findByProviderCustomerIdAndEndAtIsNull(providerCustomerId);
            if (byCustomer.isPresent()) {
                return byCustomer.get().getOrganisation().getId();
            }
        }

        return null;
    }

    private void markInvoicePaid(Map<String, Object> object) {
        Invoice invoice = resolveInvoice(object);
        if (invoice == null) {
            return;
        }
        BigDecimal amountPaid = centsToUsd(object.get("amount_paid"));
        invoice.setStatus(InvoiceStatus.PAID);
        if (amountPaid != null) {
            invoice.setTotalPaid(amountPaid);
            BigDecimal outstanding = invoice.getAmount().subtract(amountPaid);
            if (outstanding.compareTo(BigDecimal.ZERO) < 0) {
                outstanding = BigDecimal.ZERO;
            }
            invoice.setOutstandingBalance(outstanding);
        }
        invoice.setPaidAt(java.time.Instant.now());
        if (object.get("id") != null) {
            invoice.setProviderInvoiceId(String.valueOf(object.get("id")));
        }
        if (object.get("charge") != null) {
            invoice.setProviderChargeId(String.valueOf(object.get("charge")));
        }
        if (object.get("payment_intent") != null) {
            invoice.setProviderPaymentIntentId(String.valueOf(object.get("payment_intent")));
        }
        invoiceRepository.save(invoice);
    }

    private void markInvoicePaymentFailed(Map<String, Object> object) {
        Invoice invoice = resolveInvoice(object);
        if (invoice == null) {
            return;
        }
        invoice.setStatus(InvoiceStatus.PAST_DUE);
        if (object.get("id") != null) {
            invoice.setProviderInvoiceId(String.valueOf(object.get("id")));
        }
        invoiceRepository.save(invoice);
    }

    private void markInvoiceRefunded(Map<String, Object> object) {
        Invoice invoice = resolveInvoice(object);
        if (invoice == null) {
            return;
        }
        BigDecimal refunded = centsToUsd(object.get("amount_refunded"));
        if (refunded != null) {
            invoice.setRefundedAmount(refunded);
        }
        invoiceRepository.save(invoice);
    }

    private Invoice resolveInvoice(Map<String, Object> object) {
        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = object.get("metadata") instanceof Map ? (Map<String, Object>) object.get("metadata") : null;
        if (metadata != null && metadata.get("invoiceId") != null) {
            try {
                Long id = Long.valueOf(String.valueOf(metadata.get("invoiceId")));
                return invoiceRepository.findById(id).orElse(null);
            } catch (NumberFormatException ignored) {
                // fallback to provider id
            }
        }
        if (object.get("id") != null) {
            String providerInvoiceId = String.valueOf(object.get("id"));
            return invoiceRepository.findByProviderInvoiceId(providerInvoiceId);
        }
        return null;
    }

    private static BigDecimal centsToUsd(Object centsValue) {
        if (!(centsValue instanceof Number number)) {
            return null;
        }
        return BigDecimal.valueOf(number.longValue()).movePointLeft(2);
    }
}
