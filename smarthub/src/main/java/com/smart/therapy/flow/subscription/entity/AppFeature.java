package com.smart.therapy.flow.subscription.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.subscription.feature.FeatureScope;
import com.smart.therapy.flow.subscription.feature.FeatureType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import java.time.Instant;

/**
 * Global catalogue of all capabilities in the system.
 * Public schema; static definition (no organisation_id).
 */
@Entity
@Table(name = "app_features", schema = "public", indexes = {
    @Index(name = "idx_app_features_code", columnList = "code", unique = true)
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AppFeature extends BaseEntity {

    @Column(nullable = false, unique = true, length = 100)
    private String code; // e.g. BOOKING, FORMS, REPORTS, ASSESSMENTS, THERAPIST_SEATS, STORAGE_MB

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 20)
    @Builder.Default
    private FeatureScope scope = FeatureScope.TENANT;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 20)
    @Builder.Default
    private FeatureType type = FeatureType.CORE;

    @Column(name = "default_enabled", nullable = false)
    @Builder.Default
    private Boolean defaultEnabled = false;

    @Column(name = "is_deprecated", nullable = false)
    @Builder.Default
    private Boolean isDeprecated = false;

    @Column(name = "deprecated_at")
    private Instant deprecatedAt;
}
