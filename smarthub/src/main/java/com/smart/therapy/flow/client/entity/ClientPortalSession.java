package com.smart.therapy.flow.client.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "client_portal_sessions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientPortalSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @Column(name = "session_token", nullable = false, unique = true, length = 255)
    private String sessionToken;

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "last_activity", nullable = false)
    @Builder.Default
    private Instant lastActivity = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    // Security & Tracking Fields
    @Column(name = "device_fingerprint", length = 255)
    private String deviceFingerprint; // For device tracking

    @Column(name = "is_revoked", nullable = false)
    @Builder.Default
    private Boolean isRevoked = false; // For manual logout/revocation

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "revoked_by", length = 255)
    private String revokedBy; // Admin/User who revoked

    @Column(name = "revoked_reason", columnDefinition = "TEXT")
    private String revokedReason; // Security breach, manual logout, etc.

    @Column(name = "failed_login_attempts")
    private Integer failedLoginAttempts;

    @Column(name = "last_failed_login")
    private Instant lastFailedLogin;

    @Column(name = "last_successful_login")
    private Instant lastSuccessfulLogin;

    @Column(name = "login_location", length = 255)
    private String loginLocation; // City, Country from IP

    @Column(name = "failed_activity_count")
    private Integer failedActivityCount;

    // Helper methods
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(Instant.now());
    }

    public boolean isValid() {
        return Boolean.TRUE.equals(isActive) && 
               !Boolean.TRUE.equals(isRevoked) && 
               !isExpired();
    }

    public void revoke(String reason, String revokedBy) {
        this.isRevoked = true;
        this.isActive = false;
        this.revokedAt = Instant.now();
        this.revokedBy = revokedBy;
        this.revokedReason = reason;
    }

    public void updateActivity() {
        this.lastActivity = Instant.now();
    }
}

