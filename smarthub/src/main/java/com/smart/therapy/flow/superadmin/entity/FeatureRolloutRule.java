package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Dynamic feature override rules managed by platform super-admin.
 * Scope supports organisation-wide, single therapist, or therapist group targeting.
 */
@Entity
@Table(name = "feature_rollout_rules", schema = "public", indexes = {
        @Index(name = "idx_feature_rollout_org_feature", columnList = "organisation_id, feature_key"),
        @Index(name = "idx_feature_rollout_scope_target", columnList = "organisation_id, scope, target_key"),
        @Index(name = "idx_feature_rollout_active_window", columnList = "start_at, end_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class FeatureRolloutRule extends BaseEntity {

    public enum Scope {
        GLOBAL,
        ORGANISATION,
        THERAPIST
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    private Scope scope;

    @Column(name = "target_key", length = 120)
    private String targetKey;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "usage_limit")
    private Integer usageLimit;

    @Column(name = "start_at", nullable = false)
    private Instant startAt;

    @Column(name = "end_at")
    private Instant endAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public boolean isActiveAt(Instant at) {
        if (at == null) {
            at = Instant.now();
        }
        return !at.isBefore(startAt) && (endAt == null || at.isBefore(endAt));
    }
}
