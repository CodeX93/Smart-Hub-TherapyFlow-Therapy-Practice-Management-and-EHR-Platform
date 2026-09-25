package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Schema(description = "Manual subscription renewal invoice created by super-admin")
public class SuperAdminSubscriptionInvoiceResponse {
    private Long invoiceId;
    private Long subscriptionId;
    private String status;
    private BigDecimal amount;
    private BigDecimal outstandingBalance;
    private LocalDate dueDate;
    private Instant billingPeriodStart;
    private Instant billingPeriodEnd;
}
