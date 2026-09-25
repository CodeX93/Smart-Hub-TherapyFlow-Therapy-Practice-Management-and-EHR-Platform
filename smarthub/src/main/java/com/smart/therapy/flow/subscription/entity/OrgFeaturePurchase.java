package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Add-ons / feature purchases. NOT overrides — additive.
 */
@Entity
@Table(name = "org_feature_purchases", schema = "public", indexes = {
    @Index(name = "idx_org_feature_purchase_sub", columnList = "subscription_id"),
    @Index(name = "idx_org_feature_purchase_dates", columnList = "start_at, end_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrgFeaturePurchase extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private OrgSubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private AppFeature feature;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "price_per_unit_at_time", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnitAtTime;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public boolean isActiveAt(Instant at) {
        if (at == null) at = Instant.now();
        return !at.isBefore(startAt) && (endAt == null || at.isBefore(endAt));
    }
}
