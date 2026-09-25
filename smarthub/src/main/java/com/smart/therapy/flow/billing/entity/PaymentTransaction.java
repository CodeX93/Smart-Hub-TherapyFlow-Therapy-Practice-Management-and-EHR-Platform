package com.smart.therapy.flow.billing.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.smart.therapy.flow.billing.enums.PaymentSource;
import com.smart.therapy.flow.billing.enums.PaymentStatus;
import com.smart.therapy.flow.billing.enums.TransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * PaymentTransaction - stores payment provider transaction details
 * (Stripe, PayPal, Square, etc.)
 */
@Entity
@Table(name = "payment_transactions", indexes = {
    @Index(name = "idx_payment_transaction_payment", columnList = "payment_id"),
    @Index(name = "idx_payment_transaction_provider", columnList = "provider"),
    @Index(name = "idx_payment_transaction_charge_id", columnList = "provider_charge_id"),
    @Index(name = "idx_payment_transaction_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode
public class PaymentTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PaymentSource provider; // 'stripe', 'paypal', 'square', 'manual'

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 30)
    private TransactionType transactionType; // 'charge', 'refund', 'adjustment'

    @Column(precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "provider_intent_id", length = 255)
    private String providerIntentId; // Stripe Payment Intent ID

    @Column(name = "provider_charge_id", length = 255)
    private String providerChargeId; // Stripe Charge ID

    @Column(name = "provider_customer_id", length = 255)
    private String providerCustomerId; // Stripe Customer ID

    @Column(name = "provider_payment_method_id", length = 255)
    private String providerPaymentMethodId; // Stripe Payment Method ID

    @Column(name = "connected_account_id", length = 255)
    private String connectedAccountId; // Stripe connected account used for direct charge

    @Enumerated(EnumType.STRING)
    @Column(length = 30)
    private PaymentStatus status; // 'pending', 'succeeded', 'failed', 'refunded'

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "is_voided", nullable = false)
    @Builder.Default
    private Boolean isVoided = false;

    @Column(name = "voided_at")
    private Instant voidedAt;

    @Column(name = "voided_by")
    private Long voidedBy;

    @Column(name = "void_reason", columnDefinition = "TEXT")
    private String voidReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response", columnDefinition = "json")
    private JsonNode rawResponse; // Full provider response

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
        if (this.isVoided == null) {
            this.isVoided = false;
        }
    }
}
