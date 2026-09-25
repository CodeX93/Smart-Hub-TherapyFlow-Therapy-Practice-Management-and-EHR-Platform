package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.subscription.enums.RefundRetryStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refund_retry_tasks", schema = "public", indexes = {
        @Index(name = "idx_refund_retry_tasks_status_next_retry", columnList = "status,next_retry_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class RefundRetryTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "amount_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private RefundRetryStatus status = RefundRetryStatus.PENDING;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "next_retry_at", nullable = false)
    private Instant nextRetryAt;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
