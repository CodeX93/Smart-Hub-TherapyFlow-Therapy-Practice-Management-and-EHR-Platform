package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Feature flag per organisation. Used for SSO enablement, advanced billing, beta features, API gating.
 * Stored in public schema.
 */
@Entity
@Table(name = "tenant_features", schema = "public", uniqueConstraints = {
    @UniqueConstraint(name = "uq_tenant_features_org_key", columnNames = {"organisation_id", "feature_key"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TenantFeature {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "feature_key", nullable = false, length = 100)
    private String featureKey;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = Instant.now();
    }
}
