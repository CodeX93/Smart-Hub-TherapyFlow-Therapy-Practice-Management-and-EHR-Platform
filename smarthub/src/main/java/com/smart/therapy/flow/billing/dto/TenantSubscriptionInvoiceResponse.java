package com.smart.therapy.flow.billing.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
public class TenantSubscriptionInvoiceResponse {
    private Long invoiceId;
    private Long subscriptionId;
    private String status;
    private BigDecimal amount;
    private BigDecimal outstandingBalance;
    private BigDecimal totalPaid;
    private BigDecimal refundedAmount;
    private LocalDate dueDate;
    private Instant billingPeriodStart;
    private Instant billingPeriodEnd;
    private Instant paidAt;
    private String providerInvoiceId;
    private Instant createdAt;
}
