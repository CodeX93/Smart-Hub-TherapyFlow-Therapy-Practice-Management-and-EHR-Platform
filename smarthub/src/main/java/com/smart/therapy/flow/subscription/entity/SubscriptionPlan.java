package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;

/**
 * Global plan definitions (Starter, Professional, Enterprise).
 * Public schema; static definition (no organisation_id).
 */
@Entity
@Table(name = "subscription_plans", schema = "public", indexes = {
    @Index(name = "idx_subscription_plans_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SubscriptionPlan extends BaseEntity {

    @Column(nullable = false, length = 50, unique = true)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "base_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "annual_price", precision = 12, scale = 2)
    private BigDecimal annualPrice;

    @Column(name = "billing_cycle", nullable = false, length = 20)
    private String billingCycle;

    @Column(name = "trial_days")
    private Integer trialDays;

    @Column(name = "provider_price_id_monthly", length = 255)
    private String providerPriceIdMonthly;

    @Column(name = "provider_price_id_annual", length = 255)
    private String providerPriceIdAnnual;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus status = com.smart.therapy.flow.subscription.enums.SubscriptionPlanStatus.ACTIVE;
}
