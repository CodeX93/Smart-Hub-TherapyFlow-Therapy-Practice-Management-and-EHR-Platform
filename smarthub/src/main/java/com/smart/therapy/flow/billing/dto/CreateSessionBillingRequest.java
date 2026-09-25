package com.smart.therapy.flow.billing.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
public class CreateSessionBillingRequest {

    @NotNull(message = "Session ID is required")
    private Long sessionId;

    private Long serviceId;
    private String serviceCode;
    private BigDecimal unitRate;
    private Integer units = 1;
    private Boolean insuranceCovered = false;
    private Instant billingDate;
    private BigDecimal copayAmount;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
}

