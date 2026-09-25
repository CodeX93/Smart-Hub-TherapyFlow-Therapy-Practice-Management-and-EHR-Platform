package com.smart.therapy.flow.superadmin.entity;

import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_impersonation_sessions", schema = "public", indexes = {
        @Index(name = "idx_platform_impersonation_sessions_status", columnList = "status"),
        @Index(name = "idx_platform_impersonation_sessions_org", columnList = "organisation_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformImpersonationSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "super_admin_auth_id", nullable = false)
    private Long superAdminAuthId;

    @Column(name = "target_auth_id", nullable = false)
    private Long targetAuthId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organisation_id", nullable = false)
    private Organisation organisation;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "token_prefix", nullable = false, unique = true, length = 24)
    private String tokenPrefix;

    @Column(name = "token_hash", nullable = false, length = 128)
    private String tokenHash;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
