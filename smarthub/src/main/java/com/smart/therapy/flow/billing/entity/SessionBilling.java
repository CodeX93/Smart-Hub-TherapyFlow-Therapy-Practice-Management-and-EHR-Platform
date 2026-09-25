package com.smart.therapy.flow.billing.entity;

import com.smart.therapy.flow.billing.enums.BillingStatus;
import com.smart.therapy.flow.billing.enums.DiscountType;
import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.session.entity.Session;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "session_billing", indexes = {
    @Index(name = "idx_session_billing_session", columnList = "session_id"),
    @Index(name = "idx_session_billing_status", columnList = "billing_status"),
    @Index(name = "idx_session_billing_date", columnList = "billing_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SessionBilling extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, unique = true)
    @ToString.Exclude
    private Session session;

    @Column(name = "service_code", nullable = false, length = 20)
    private String serviceCode;

    @Column(nullable = false)
    @Builder.Default
    private Integer units = 1;

    @Column(name = "rate_per_unit", nullable = false, precision = 10, scale = 2)
    private BigDecimal ratePerUnit;

    /** Service catalog rate before invoice-policy adjustment (nullable for legacy rows). */
    @Column(name = "original_rate_per_unit", precision = 10, scale = 2)
    private BigDecimal originalRatePerUnit;

    @Column(name = "subtotal_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotalAmount;

    /** Service catalog subtotal (rate × units) before invoice-policy adjustment. */
    @Column(name = "original_subtotal_amount", precision = 10, scale = 2)
    private BigDecimal originalSubtotalAmount;

    @Column(name = "insurance_covered", nullable = false)
    @Builder.Default
    private Boolean insuranceCovered = false;

    @Column(name = "insurance_amount", precision = 10, scale = 2)
    private BigDecimal insuranceAmount;

    @Column(name = "copay_amount", precision = 10, scale = 2)
    private BigDecimal copayAmount;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal paidAmount = BigDecimal.ZERO;

    @Column(name = "client_paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal clientPaidAmount = BigDecimal.ZERO;

    @Column(name = "insurance_paid_amount", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal insurancePaidAmount = BigDecimal.ZERO;

    @Column(name = "outstanding_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal outstandingAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_status", nullable = false, length = 30)
    @Builder.Default
    private BillingStatus billingStatus = BillingStatus.PENDING;

    @Column(name = "billing_date")
    private java.time.LocalDate billingDate;

    @Column(name = "due_date")
    private java.time.LocalDate dueDate;

    // Discount fields
    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", precision = 10, scale = 2)
    private BigDecimal discountValue; // % value (e.g., 10.00 for 10%) or $ amount

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "invoice_policy_id")
    private Long invoicePolicyId;

    @Column(name = "stripe_checkout_session_id", length = 255)
    private String stripeCheckoutSessionId;

    @Column(name = "stripe_payment_intent_id", length = 255)
    private String stripePaymentIntentId;
}
