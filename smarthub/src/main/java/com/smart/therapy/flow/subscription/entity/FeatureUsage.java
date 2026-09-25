package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Usage tracking per subscription per feature per period.
 */
@Entity
@Table(name = "feature_usage", schema = "public", indexes = {
    @Index(name = "idx_feature_usage_sub", columnList = "subscription_id"),
    @Index(name = "idx_feature_usage_period", columnList = "period_start, period_end"),
    @Index(name = "idx_feature_usage_sub_feature_period", columnList = "subscription_id, feature_id, period_start, target_key", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FeatureUsage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private OrgSubscription subscription;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "feature_id", nullable = false)
    private AppFeature feature;

    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Long usageCount = 0L;

    @Column(name = "target_key", length = 64)
    private String targetKey;

    @Column(name = "period_start", nullable = false)
    private Instant periodStart;

    @Column(name = "period_end", nullable = false)
    private Instant periodEnd;
}
