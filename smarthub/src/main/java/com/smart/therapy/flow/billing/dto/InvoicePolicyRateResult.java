package com.smart.therapy.flow.billing.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

@Value
@Builder
public class InvoicePolicyRateResult {
    BigDecimal ratePerUnit;
    Long invoicePolicyId;
    boolean policyApplied;

    public static InvoicePolicyRateResult fromBaseRate(BigDecimal baseRate) {
        return InvoicePolicyRateResult.builder()
                .ratePerUnit(baseRate)
                .invoicePolicyId(null)
                .policyApplied(false)
                .build();
    }
}
