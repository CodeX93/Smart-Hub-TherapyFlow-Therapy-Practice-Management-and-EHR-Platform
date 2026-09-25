package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Enterprise session control: jti, revoke, force logout, audit.
 */
@Entity
@Table(name = "auth_sessions", schema = "public", indexes = {
    @Index(name = "idx_auth_sessions_auth_id", columnList = "auth_id"),
    @Index(name = "idx_auth_sessions_expires", columnList = "expires_at")
}, uniqueConstraints = {
    @UniqueConstraint(name = "uq_auth_sessions_jwt_id", columnNames = "jwt_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AuthSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false)
    private AuthIdentity authIdentity;

    @Column(name = "jwt_id", nullable = false, unique = true, length = 100)
    private String jwtId;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_activity_at", nullable = false)
    private Instant lastActivityAt;

    @Column(name = "idle_expires_at", nullable = false)
    private Instant idleExpiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "impersonation_session_id")
    private Long impersonationSessionId;
}
