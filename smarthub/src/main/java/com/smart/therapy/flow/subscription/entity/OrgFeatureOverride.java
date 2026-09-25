package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

@Entity
@Table(name = "org_feature_overrides", schema = "public", indexes = {
        @Index(name = "idx_org_feature_overrides_org", columnList = "organisation_id"),
        @Index(name = "idx_org_feature_overrides_org_feature", columnList = "organisation_id, feature_key")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_org_feature_overrides_org_feature", columnNames = {"organisation_id", "feature_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class OrgFeatureOverride extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "usage_limit")
    private Integer usageLimit;
}
