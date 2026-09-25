package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Invoices are tied to a subscription, not to a plan.
 * Audit-safe: you can explain any invoice historically.
 */
@Entity
@Table(name = "invoices", schema = "public", indexes = {
    @Index(name = "idx_invoices_subscription", columnList = "subscription_id"),
    @Index(name = "idx_invoices_status", columnList = "status"),
    @Index(name = "idx_invoices_due_date", columnList = "due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Invoice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private OrgSubscription subscription;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "outstanding_balance", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal outstandingBalance = BigDecimal.ZERO;

    @Column(name = "total_paid", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal totalPaid = BigDecimal.ZERO;

    @Column(name = "refunded_amount", nullable = false, precision = 12, scale = 2)
    @Builder.Default
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(name = "billing_period_start", nullable = false)
    private Instant billingPeriodStart;

    @Column(name = "billing_period_end", nullable = false)
    private Instant billingPeriodEnd;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private InvoiceStatus status = InvoiceStatus.PENDING;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "provider_invoice_id", length = 120)
    private String providerInvoiceId;

    @Column(name = "provider_charge_id", length = 120)
    private String providerChargeId;

    @Column(name = "provider_payment_intent_id", length = 120)
    private String providerPaymentIntentId;
}
