package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.subscription.enums.InvoiceDisputeStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice_disputes", schema = "public", indexes = {
        @Index(name = "idx_invoice_disputes_invoice", columnList = "invoice_id, status, opened_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class InvoiceDispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Column(name = "external_case_id", length = 120)
    private String externalCaseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InvoiceDisputeStatus status = InvoiceDisputeStatus.OPEN;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Column(name = "amount_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal amountUsd;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private Long createdBy;
}
