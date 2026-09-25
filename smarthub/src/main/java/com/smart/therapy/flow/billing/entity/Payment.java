package com.smart.therapy.flow.billing.entity;

import com.smart.therapy.flow.billing.enums.PaymentMethod;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Payment - represents a payment made towards a session billing (tenant schema).
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_billing", columnList = "session_billing_id"),
    @Index(name = "idx_payment_status", columnList = "status"),
    @Index(name = "idx_payment_method", columnList = "payment_method"),
    @Index(name = "idx_payment_date", columnList = "payment_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_billing_id", nullable = false)
    private SessionBilling sessionBilling;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 50)
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_source", length = 50)
    private PaymentSource paymentSource;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status;

    @Column(name = "payment_date")
    private Instant paymentDate;

    @Column(length = 100)
    private String reference; // Transaction reference number

    @Column(columnDefinition = "TEXT")
    private String notes;
}
