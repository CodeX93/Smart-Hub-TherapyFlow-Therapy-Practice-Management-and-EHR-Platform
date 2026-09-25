package com.smart.therapy.flow.billing.dto;

import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.validation.PaymentDateConstraint;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Schema(description = "Request to record a payment for a billing record")
public class RecordPaymentRequest {

    @Schema(description = "Optional client ID for ownership verification")
    private Long clientId;

    @NotNull(message = "Payment amount is required")
    @Positive(message = "Payment amount must be positive")
    @Schema(description = "Cumulative amount paid for this source (client or insurance). Server records the delta in payment_transactions.", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal paymentAmount;

    @NotNull(message = "Payment method is required")
    @Schema(description = "Payment method", example = "CREDIT_CARD", requiredMode = Schema.RequiredMode.REQUIRED)
    private PaymentMethod paymentMethod;

    @Schema(description = "Reference number (transaction ID, check number, etc.)", example = "CHK-12345")
    private String referenceNumber;

    @Schema(description = "Additional payment notes", example = "Payment received via check #12345")
    private String notes;

    @PaymentDateConstraint
    @Schema(description = "Optional explicit payment date. Defaults to current time when omitted. Cannot be in the future or more than 2 years in the past.")
    private Instant paymentDate;

    @Schema(description = "Optional payment side/source hint", example = "insurance", allowableValues = { "client", "insurance" })
    private String paymentSide;

    @Schema(description = "Optimistic concurrency: prior cumulative paid for this source as shown in the UI", example = "40.00")
    private BigDecimal expectedPreviousForSource;

    @Schema(description = "Allow recording payment for a zero-balance bill (requires overrideReason)", example = "false")
    private Boolean allowZeroBillOverpayment;

    @Schema(description = "Mandatory reason when overriding zero-balance guard", example = "Advance deposit for upcoming sessions")
    private String overrideReason;
}
