package com.smart.therapy.flow.billing.dto;

import com.smart.therapy.flow.billing.enums.PaymentMethod;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class SplitPaymentLegRequest {

    @Schema(description = "Payment amount for this leg", example = "40.00")
    private BigDecimal amount;

    @Schema(description = "Payment method for this leg", example = "INSURANCE")
    private PaymentMethod paymentMethod;

    @Schema(description = "Optional payment date. Defaults to current time when omitted.")
    private Instant paymentDate;

    @Schema(description = "Reference/check/EOB number", example = "EOB-123")
    private String referenceNumber;
}

