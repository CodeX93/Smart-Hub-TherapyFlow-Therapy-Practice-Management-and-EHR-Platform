package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Billing history entry")
public class BillingHistoryResponse {

    @Schema(description = "Billing record ID", example = "123")
    private Long billingId;

    @Schema(description = "Client ID", example = "456")
    private Long clientId;

    @Schema(description = "Client MRN / client number", example = "CL-2026-0001")
    private String clientMrn;
    private String clientReferenceNumber;

    @Schema(description = "Client name", example = "John Doe")
    private String clientName;

    @Schema(description = "Session ID", example = "789")
    private Long sessionId;

    @Schema(description = "Session date", example = "2024-01-15T10:00:00Z")
    private Instant sessionDate;

    @Schema(description = "Session/schedule status option key (e.g. no-show, completed)", example = "no-show")
    private String sessionStatus;

    @Schema(description = "Service code", example = "PSY-60")
    private String serviceCode;

    @Schema(description = "Service name", example = "Psychotherapy Session - 60 minutes")
    private String serviceName;

    @Schema(description = "Total amount after invoice policy", example = "75.00")
    private BigDecimal totalAmount;

    @Schema(description = "Service list subtotal before invoice policy", example = "150.00")
    private BigDecimal originalSubtotalAmount;

    @Schema(description = "Discount amount", example = "22.50")
    private BigDecimal discountAmount;

    @Schema(description = "Amount due after discount", example = "127.50")
    private BigDecimal amountDue;

    @Schema(description = "Remaining amount due after payments", example = "30.00")
    private BigDecimal remainingDue;

    @Schema(description = "Payment status", example = "paid")
    private String paymentStatus;

    @Schema(description = "Billing status", example = "billed")
    private String billingStatus;

    @Schema(description = "Payment method", example = "credit_card")
    private String paymentMethod;

    @Schema(description = "Payment amount", example = "127.50")
    private BigDecimal paymentAmount;

    @Schema(description = "Payment date", example = "2024-01-20T14:30:00Z")
    private Instant paymentDate;

    @Schema(description = "Invoice policy ID when a policy adjusted the rate", example = "12")
    private Long invoicePolicyId;

    @Schema(description = "Billing date", example = "2024-01-15T00:00:00Z")
    private Instant billingDate;

    @Schema(description = "Created at", example = "2024-01-15T08:00:00Z")
    private Instant createdAt;
}
