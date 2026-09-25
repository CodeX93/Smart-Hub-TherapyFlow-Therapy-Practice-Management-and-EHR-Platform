package com.smart.therapy.flow.billing.dto;

import com.smart.therapy.flow.billing.enums.InvoicePolicyPriceType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class InvoicePolicyResponse {
    private Long id;
    private String clientTypeKey;
    private String clientTypeLabel;
    private String appointmentStatusKey;
    private String appointmentStatusLabel;
    private Boolean enabled;
    private InvoicePolicyPriceType priceType;
    private BigDecimal invoicePrice;
    private String policyName;
    private Long serviceId;
    private String serviceScopeKey;
    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;
    private Integer priority;
    private Instant createdAt;
    private Instant updatedAt;
}

