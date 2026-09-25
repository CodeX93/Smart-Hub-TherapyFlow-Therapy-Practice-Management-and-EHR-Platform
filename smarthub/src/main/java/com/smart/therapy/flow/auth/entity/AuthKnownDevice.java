package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_known_devices", schema = "public", indexes = {
        @Index(name = "idx_auth_known_devices_identity", columnList = "auth_identity_id, last_seen_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_known_devices_identity_fp",
                columnNames = {"auth_identity_id", "fingerprint_hash"})
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "createdat", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updatedat", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class AuthKnownDevice extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_identity_id", nullable = false)
    private AuthIdentity authIdentity;

    @Column(name = "fingerprint_hash", nullable = false, length = 64)
    private String fingerprintHash;

    @Column(name = "device_label", nullable = false, length = 120)
    private String deviceLabel;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "last_ip", length = 50)
    private String lastIp;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    @Column(name = "last_notified_at")
    private Instant lastNotifiedAt;

    /** When true and not expired/revoked, MFA may be skipped on password login. */
    @Column(name = "trusted", nullable = false)
    @Builder.Default
    private Boolean trusted = false;

    @Column(name = "trusted_at")
    private Instant trustedAt;

    @Column(name = "trust_expires_at")
    private Instant trustExpiresAt;

    @Column(name = "trust_revoked_at")
    private Instant trustRevokedAt;

    /** SHA-256 hex of the opaque device trust token returned to the client once. */
    @Column(name = "trust_token_hash", length = 64)
    private String trustTokenHash;

    public boolean isTrustActive(Instant now) {
        Instant at = now != null ? now : Instant.now();
        return Boolean.TRUE.equals(trusted)
                && trustRevokedAt == null
                && trustExpiresAt != null
                && trustExpiresAt.isAfter(at)
                && trustTokenHash != null
                && !trustTokenHash.isBlank();
    }
}
