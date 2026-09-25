package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_impersonation_policy", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformImpersonationPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Boolean enabled;

    @Column(name = "require_reason", nullable = false)
    private Boolean requireReason;

    @Column(name = "min_reason_length", nullable = false)
    private Integer minReasonLength;

    @Column(name = "max_duration_minutes", nullable = false)
    private Integer maxDurationMinutes;

    @Column(name = "allow_cross_organisation", nullable = false)
    private Boolean allowCrossOrganisation;

    @Column(name = "allowed_role_names", columnDefinition = "TEXT")
    private String allowedRoleNames;

    @Column(name = "denied_role_names", columnDefinition = "TEXT")
    private String deniedRoleNames;

    @Column(name = "allowed_org_ids", columnDefinition = "TEXT")
    private String allowedOrgIds;

    @Column(name = "denied_org_ids", columnDefinition = "TEXT")
    private String deniedOrgIds;

    @Column(name = "updated_by_auth_id")
    private Long updatedByAuthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
