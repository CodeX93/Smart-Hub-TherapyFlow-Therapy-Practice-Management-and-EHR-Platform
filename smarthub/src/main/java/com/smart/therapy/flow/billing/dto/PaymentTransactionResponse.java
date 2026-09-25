package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment transaction response")
public class PaymentTransactionResponse {

    @Schema(description = "Transaction ID", example = "1")
    private Long id;

    @Schema(description = "Payment provider", example = "stripe", allowableValues = { "stripe", "paypal", "square",
            "manual" })
    private String provider;

    @Schema(description = "Transaction type", example = "charge", allowableValues = { "charge", "refund",
            "adjustment" })
    private String transactionType;

    @Schema(description = "Transaction amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Provider payment intent ID", example = "pi_1234567890")
    private String providerIntentId;

    @Schema(description = "Provider charge ID", example = "ch_1234567890")
    private String providerChargeId;

    @Schema(description = "Provider customer ID", example = "cus_1234567890")
    private String providerCustomerId;

    @Schema(description = "Provider payment method ID", example = "pm_1234567890")
    private String providerPaymentMethodId;

    @Schema(description = "Transaction status", example = "succeeded", allowableValues = { "pending", "succeeded",
            "failed", "refunded" })
    private String status;

    @Schema(description = "Failure reason if transaction failed")
    private String failureReason;

    @Schema(description = "Whether this transaction has been voided")
    private Boolean voided;

    @Schema(description = "Void reason when transaction is voided")
    private String voidReason;

    @Schema(description = "Transaction voided timestamp")
    private Instant voidedAt;

    @Schema(description = "Transaction created timestamp")
    private Instant createdAt;
}
