package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Payment record response")
public class PaymentResponse {

    @Schema(description = "Payment ID", example = "1")
    private Long id;

    @Schema(description = "Session billing ID", example = "123")
    private Long sessionBillingId;

    @Schema(description = "Payment amount", example = "150.00")
    private BigDecimal amount;

    @Schema(description = "Payment method", example = "credit_card", allowableValues = { "credit_card", "cash",
            "check", "debit_card", "insurance", "bank_transfer", "online_payment", "credit_balance" })
    private String paymentMethod;

    @Schema(description = "Payment source", example = "stripe", allowableValues = { "stripe", "manual",
            "insurance_portal", "credit_balance_transfer" })
    private String paymentSource;

    @Schema(description = "Payment status", example = "succeeded", allowableValues = { "pending", "succeeded", "failed",
            "refunded" })
    private String status;

    @Schema(description = "Payment date", example = "2024-01-20T14:30:00Z")
    private Instant paymentDate;

    @Schema(description = "Transaction reference number", example = "pi_1234567890")
    private String reference;

    @Schema(description = "Payment notes")
    private String notes;

    @Schema(description = "Created timestamp")
    private Instant createdAt;

    @Schema(description = "Updated timestamp")
    private Instant updatedAt;

    @Schema(description = "Associated payment transactions")
    private List<PaymentTransactionResponse> transactions;
}
