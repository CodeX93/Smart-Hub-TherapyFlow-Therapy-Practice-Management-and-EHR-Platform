package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentStatus;
import com.smart.therapy.flow.subscription.enums.InvoiceAdjustmentType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "invoice_adjustments", schema = "public", indexes = {
        @Index(name = "idx_invoice_adjustments_invoice", columnList = "invoice_id, adjustment_type, created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = false)
public class InvoiceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "invoice_id", nullable = false)
    private Invoice invoice;

    @Enumerated(EnumType.STRING)
    @Column(name = "adjustment_type", nullable = false, length = 30)
    private InvoiceAdjustmentType adjustmentType;

    @Column(name = "amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private InvoiceAdjustmentStatus status = InvoiceAdjustmentStatus.APPROVED;

    @Column(name = "provider_ref_id", length = 120)
    private String providerRefId;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
