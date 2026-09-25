package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * One-time SSO authorization state for CSRF/replay protection.
 */
@Entity
@Table(name = "organisation_sso_states", schema = "public", indexes = {
    @Index(name = "idx_org_sso_states_org_provider", columnList = "organisation_id,provider"),
    @Index(name = "idx_org_sso_states_expires", columnList = "expires_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganisationSsoState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "state_token", nullable = false, unique = true, length = 255)
    private String stateToken;

    @Column(name = "organisation_id", nullable = false)
    private Long organisationId;

    @Column(name = "provider", nullable = false, length = 30)
    private String provider;

    @Column(name = "redirect_uri", nullable = false, length = 500)
    private String redirectUri;

    @Column(name = "nonce", nullable = false, length = 255)
    private String nonce;

    @Column(name = "pkce_verifier", nullable = false, length = 255)
    private String pkceVerifier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @PrePersist
    public void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }
}

