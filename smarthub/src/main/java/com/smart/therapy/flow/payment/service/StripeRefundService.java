package com.smart.therapy.flow.payment.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.subscription.entity.Invoice;
import com.stripe.model.Refund;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class StripeRefundService {

    private final StripePlatformConfigService stripePlatformConfigService;

    public String createRefund(Invoice invoice, BigDecimal amountUsd, String reason) {
        if (invoice.getProviderPaymentIntentId() == null && invoice.getProviderChargeId() == null) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "REFUND_SOURCE_NOT_FOUND",
                    "Invoice has no provider payment source to refund");
        }
        try {
            com.stripe.Stripe.apiKey = stripePlatformConfigService.requirePlatformSecretKey();

            long cents = amountUsd.movePointRight(2).longValueExact();
            RefundCreateParams.Builder builder = RefundCreateParams.builder()
                    .setAmount(cents)
                    .putMetadata("invoiceId", String.valueOf(invoice.getId()))
                    .putMetadata("reason", reason != null ? reason : "");

            if (invoice.getProviderPaymentIntentId() != null && !invoice.getProviderPaymentIntentId().isBlank()) {
                builder.setPaymentIntent(invoice.getProviderPaymentIntentId());
            } else {
                builder.setCharge(invoice.getProviderChargeId());
            }
            Refund refund = Refund.create(builder.build());
            return refund.getId();
        } catch (Exception ex) {
            throw new StoryApiException(HttpStatus.SERVICE_UNAVAILABLE, "STRIPE_UNAVAILABLE", "Stripe refund API unavailable");
        }
    }
}
