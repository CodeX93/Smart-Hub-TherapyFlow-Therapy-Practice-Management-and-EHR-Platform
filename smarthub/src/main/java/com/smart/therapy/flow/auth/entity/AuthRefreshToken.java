package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_refresh_tokens", schema = "public", indexes = {
        @Index(name = "idx_auth_refresh_tokens_auth_id", columnList = "auth_id"),
        @Index(name = "idx_auth_refresh_tokens_family_id", columnList = "family_id"),
        @Index(name = "idx_auth_refresh_tokens_expires", columnList = "expires_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_auth_refresh_tokens_hash", columnNames = "token_hash")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AuthRefreshToken extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_id", nullable = false)
    private AuthIdentity authIdentity;

    @Column(name = "family_id", nullable = false, length = 64)
    private String familyId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked", nullable = false)
    @Builder.Default
    private Boolean revoked = false;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_hash", length = 64)
    private String replacedByHash;

    @Column(name = "reuse_detected", nullable = false)
    @Builder.Default
    private Boolean reuseDetected = false;

    @Column(name = "ip_address", length = 50)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
