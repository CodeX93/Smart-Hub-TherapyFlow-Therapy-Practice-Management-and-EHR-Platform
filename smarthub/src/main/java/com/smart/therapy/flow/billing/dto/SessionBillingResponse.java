package com.smart.therapy.flow.billing.dto;

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
public class SessionBillingResponse {

        private Long id;
        private Long sessionId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Actual session start time (UTC instant)")
        private Instant sessionDate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Session/schedule status option key (e.g. no-show, completed)", example = "no-show")
        private String sessionStatus;
        private Long clientId;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Client MRN / client number (e.g. CL-2026-0001)")
        private String clientMrn;
    private String clientReferenceNumber;
        private String clientName;
        private Long therapistId;
        private String therapistName;
        private Long serviceId;
        private String serviceCode;
        private String serviceName;
        private BigDecimal unitRate;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Service list rate before invoice policy")
        private BigDecimal originalRatePerUnit;
        private Integer units;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Service list subtotal before invoice policy")
        private BigDecimal originalSubtotalAmount;
        private BigDecimal totalAmount;
        private Boolean insuranceCovered;
        // Removed paymentStatus and stripe fields as they are not in SessionBilling
        // entity
        // private String paymentStatus;
        // private String stripePaymentIntentId;
        // ...

        private String billingStatus;
        @io.swagger.v3.oas.annotations.media.Schema(description = "Derived payment outcome: unpaid, partial, paid, denied, cancelled")
        private String paymentStatus;
        private String paymentMethod;
        private Instant paymentDate;
        private Instant billingDate;
        private BigDecimal copayAmount;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Type of discount applied", example = "percentage", allowableValues = {
                        "percentage", "fixed" })
        private String discountType;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Discount value. For 'percentage': the percentage (e.g., 15.00 for 15%). For 'fixed': the dollar amount.", example = "15.00")
        private BigDecimal discountValue;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Calculated discount amount in dollars deducted from total", example = "22.50")
        private BigDecimal discountAmount;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Final amount due after applying discount (totalAmount - discountAmount)", example = "127.50")
        private BigDecimal amountDue;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Remaining due after payments (never negative)", example = "32.50")
        private BigDecimal remainingDue;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Credit amount from overpayment", example = "138.00")
        private BigDecimal creditAmount;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Amount paid by client directly", example = "80.00")
        private BigDecimal clientPaidAmount;

        @io.swagger.v3.oas.annotations.media.Schema(description = "Amount paid by insurance", example = "40.00")
        private BigDecimal insurancePaidAmount;

        private Long invoicePolicyId;
        private String stripeCheckoutSessionId;
        private String stripePaymentIntentId;

        private Instant createdAt;
        private Instant updatedAt;
}
