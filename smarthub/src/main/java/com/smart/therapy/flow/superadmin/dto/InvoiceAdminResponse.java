package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import lombok.Data;

@Schema(description = "Invoice response for super-admin billing ledger")
@Data
public class InvoiceAdminResponse {
private Long invoiceId;
private Long organisationId;
private String organisationName;
private Long subscriptionId;
private String planCode;
private String planName;
private String billingCycle;
private String status;
private BigDecimal amount;
private BigDecimal outstandingBalance;
private BigDecimal totalPaid;
private BigDecimal refundedAmount;
private LocalDate dueDate;
private Instant billingPeriodStart;
private Instant billingPeriodEnd;
private Instant paidAt;
private Instant createdAt;
private String providerInvoiceId;
private String providerChargeId;
private String providerPaymentIntentId;
}
