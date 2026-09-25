package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Current price per unit for a feature (overage & add-ons).
 */
@Entity
@Table(name = "feature_pricing", schema = "public", indexes = {
    @Index(name = "idx_feature_pricing_feature", columnList = "feature_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FeaturePricing extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private AppFeature feature;

    @Column(name = "price_per_unit", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "billing_cycle", nullable = false, length = 20)
    @Builder.Default
    private AddonBillingCycle billingCycle = AddonBillingCycle.MONTHLY;

    @Column(name = "unit_value", nullable = false)
    @Builder.Default
    private Integer unitValue = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AddonCatalogStatus status = AddonCatalogStatus.ACTIVE;
}
