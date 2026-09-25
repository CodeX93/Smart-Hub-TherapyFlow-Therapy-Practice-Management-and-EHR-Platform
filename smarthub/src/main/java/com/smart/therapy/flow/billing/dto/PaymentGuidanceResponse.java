package com.smart.therapy.flow.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class PaymentGuidanceResponse {
    private Long billingId;
    private BigDecimal originalSubtotalAmount;
    private BigDecimal amountAfterPolicy;
    private BigDecimal amountAfterDiscount;
    private BigDecimal expectedClientPortion;
    private BigDecimal expectedInsurancePortion;
    private BigDecimal clientAlreadyPaid;
    private BigDecimal insuranceAlreadyPaid;
    private BigDecimal clientRemaining;
    private BigDecimal insuranceRemaining;
    private BigDecimal totalAlreadyPaid;
    private BigDecimal totalRemainingDue;
    private BigDecimal overpayDelta;
    private Boolean insuranceCovered;
}

