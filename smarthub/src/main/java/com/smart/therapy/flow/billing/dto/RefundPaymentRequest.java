package com.smart.therapy.flow.billing.dto;

import com.smart.therapy.flow.billing.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to refund a payment for a billing record")
public class RefundPaymentRequest {

    @NotNull(message = "Refund amount is required")
    @Positive(message = "Refund amount must be positive")
    @Schema(description = "Refund amount", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal refundAmount;

    @Schema(description = "Reference number for the refund", example = "REF-12345")
    private String referenceNumber;

    @Schema(description = "Refund method (defaults to BANK_TRANSFER if omitted)", example = "BANK_TRANSFER")
    private PaymentMethod paymentMethod;

    @Schema(description = "Reason/notes for refund", example = "Duplicate charge refunded")
    private String notes;
}
