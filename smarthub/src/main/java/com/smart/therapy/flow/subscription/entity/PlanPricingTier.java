package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Role-based pricing tiers per plan.
 */
@Entity
@Table(name = "plan_pricing_tiers", schema = "public", indexes = {
        @Index(name = "idx_plan_pricing_tiers_plan", columnList = "plan_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PlanPricingTier extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @Column(name = "min_therapists", nullable = false)
    private Integer minTherapists;

    @Column(name = "max_therapists", nullable = false)
    private Integer maxTherapists;

    @Column(name = "price_per_therapist_usd", nullable = false, precision = 12, scale = 2)
    private BigDecimal pricePerTherapistUsd;

    @Column(name = "included_supervisors", nullable = false)
    private Integer includedSupervisors;

    @Column(name = "included_clients", nullable = false)
    private Integer includedClients;
}
