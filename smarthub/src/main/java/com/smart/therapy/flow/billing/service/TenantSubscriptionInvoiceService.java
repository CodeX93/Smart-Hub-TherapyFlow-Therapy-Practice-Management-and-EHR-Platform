package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.billing.dto.TenantSubscriptionInvoicePayResponse;
import com.smart.therapy.flow.billing.dto.TenantSubscriptionInvoiceResponse;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.payment.service.StripePlatformConfigService;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSubscriptionInvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final StripePlatformConfigService stripePlatformConfigService;

    @Transactional(readOnly = true)
    public Page<TenantSubscriptionInvoiceResponse> listInvoices(Long organisationId, String status, int page, int size) {
        Page<Invoice> result = invoiceRepository.search(
                organisationId,
                parseInvoiceStatus(status),
                PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 200), Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        return result.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public TenantSubscriptionInvoicePayResponse createStripePaymentUrl(Long organisationId, Long invoiceId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new StoryApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found"));
        validateInvoiceOwnership(organisationId, invoice);
        validateInvoicePayable(invoice);
        if (!StringUtils.hasText(invoice.getProviderInvoiceId())) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PROVIDER_INVOICE_NOT_READY",
                    "Stripe invoice is not linked yet for this subscription invoice");
        }

        RequestOptions requestOptions = RequestOptions.builder()
                .setApiKey(stripePlatformConfigService.requirePlatformSecretKey())
                .build();

        try {
            com.stripe.model.Invoice stripeInvoice = com.stripe.model.Invoice.retrieve(invoice.getProviderInvoiceId(), requestOptions);
            if (stripeInvoice != null && "draft".equalsIgnoreCase(stripeInvoice.getStatus())) {
                stripeInvoice = stripeInvoice.finalizeInvoice(requestOptions);
            }

            String paymentUrl = stripeInvoice != null ? stripeInvoice.getHostedInvoiceUrl() : null;
            if (!StringUtils.hasText(paymentUrl) && stripeInvoice != null) {
                paymentUrl = stripeInvoice.getInvoicePdf();
            }
            if (!StringUtils.hasText(paymentUrl)) {
                throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "PAYMENT_URL_NOT_AVAILABLE",
                        "Stripe payment URL is not available for this invoice yet");
            }

            return TenantSubscriptionInvoicePayResponse.builder()
                    .invoiceId(invoice.getId())
                    .providerInvoiceId(invoice.getProviderInvoiceId())
                    .providerStatus(stripeInvoice != null ? stripeInvoice.getStatus() : null)
                    .paymentUrl(paymentUrl)
                    .build();
        } catch (StoryApiException ex) {
            throw ex;
        } catch (StripeException ex) {
            log.error("Failed to create Stripe payment URL for org={}, invoiceId={}: {}", organisationId, invoiceId, ex.getMessage());
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE",
                    "Stripe payment URL creation failed");
        }
    }

    private void validateInvoiceOwnership(Long organisationId, Invoice invoice) {
        Long invoiceOrgId = invoice.getSubscription() != null && invoice.getSubscription().getOrganisation() != null
                ? invoice.getSubscription().getOrganisation().getId()
                : null;
        if (invoiceOrgId == null || !invoiceOrgId.equals(organisationId)) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "INVOICE_NOT_FOUND", "Invoice not found");
        }
    }

    private void validateInvoicePayable(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVOICE_ALREADY_PAID", "Invoice is already paid");
        }
        if (invoice.getStatus() == InvoiceStatus.VOID) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "INVOICE_VOID", "Invoice is void");
        }
    }

    private TenantSubscriptionInvoiceResponse toResponse(Invoice invoice) {
        return TenantSubscriptionInvoiceResponse.builder()
                .invoiceId(invoice.getId())
                .subscriptionId(invoice.getSubscription() != null ? invoice.getSubscription().getId() : null)
                .status(invoice.getStatus() != null ? invoice.getStatus().name() : null)
                .amount(invoice.getAmount())
                .outstandingBalance(invoice.getOutstandingBalance())
                .totalPaid(invoice.getTotalPaid())
                .refundedAmount(invoice.getRefundedAmount())
                .dueDate(invoice.getDueDate())
                .billingPeriodStart(invoice.getBillingPeriodStart())
                .billingPeriodEnd(invoice.getBillingPeriodEnd())
                .paidAt(invoice.getPaidAt())
                .providerInvoiceId(invoice.getProviderInvoiceId())
                .createdAt(invoice.getCreatedAt())
                .build();
    }

    private InvoiceStatus parseInvoiceStatus(String status) {
        if (!StringUtils.hasText(status) || "all".equalsIgnoreCase(status.trim())) {
            return null;
        }
        String normalized = status.trim().toUpperCase(Locale.ROOT);
        if ("OVERDUE".equals(normalized)) {
            normalized = "PAST_DUE";
        }
        try {
            return InvoiceStatus.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "INVALID_INVOICE_STATUS",
                    "status must be one of all,pending,paid,failed,past_due,void");
        }
    }
}
