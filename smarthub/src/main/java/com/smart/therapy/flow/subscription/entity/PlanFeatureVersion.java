package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Versioned plan → feature mapping. What "Pro" includes can change over time
 * without breaking history. effective_from / effective_to make everything time-aware.
 */
@Entity
@Table(name = "plan_feature_versions", schema = "public", indexes = {
    @Index(name = "idx_plan_feature_plan", columnList = "plan_id"),
    @Index(name = "idx_plan_feature_effective", columnList = "effective_from, effective_to")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class PlanFeatureVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "plan_id", nullable = false)
    private SubscriptionPlan plan;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private AppFeature feature;

    @Column(name = "is_enabled", nullable = false)
    @Builder.Default
    private Boolean isEnabled = true;

    @Column(name = "usage_limit")
    private Integer usageLimit; // e.g. 500 bookings/month, null = unlimited

    @Column(name = "is_trial_available", nullable = false)
    @Builder.Default
    private Boolean isTrialAvailable = false;

    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    @Column(name = "effective_to") // null = currently active
    private Instant effectiveTo;

    /**
     * True if this version is active at the given time.
     */
    public boolean isEffectiveAt(Instant at) {
        if (at == null) at = Instant.now();
        return !at.isBefore(effectiveFrom) && (effectiveTo == null || at.isBefore(effectiveTo));
    }
}
