package com.smart.therapy.flow.organisation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "org_stripe_accounts", schema = "public", indexes = {
        @Index(name = "idx_org_stripe_accounts_org", columnList = "organisation_id"),
        @Index(name = "idx_org_stripe_accounts_connect", columnList = "connect_account_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uq_org_stripe_accounts_org", columnNames = "organisation_id"),
        @UniqueConstraint(name = "uq_org_stripe_accounts_connect", columnNames = "connect_account_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrgStripeAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organisation_id", nullable = false, unique = true)
    private Long organisationId;

    @Column(name = "connect_account_id", unique = true, length = 255)
    private String connectAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "onboarding_status", nullable = false, length = 30)
    @Builder.Default
    private OrgStripeOnboardingStatus onboardingStatus = OrgStripeOnboardingStatus.NOT_CONNECTED;

    @Column(name = "charges_enabled", nullable = false)
    @Builder.Default
    private Boolean chargesEnabled = false;

    @Column(name = "payouts_enabled", nullable = false)
    @Builder.Default
    private Boolean payoutsEnabled = false;

    @Column(name = "details_submitted", nullable = false)
    @Builder.Default
    private Boolean detailsSubmitted = false;

    @Column(name = "country", length = 2)
    private String country;

    @Column(name = "default_currency", length = 3)
    private String defaultCurrency;

    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;

    @Column(name = "disabled_reason", columnDefinition = "TEXT")
    private String disabledReason;

    @Column(name = "oauth_state", length = 255)
    private String oauthState;

    @Column(name = "oauth_state_expires_at")
    private Instant oauthStateExpiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (onboardingStatus == null) {
            onboardingStatus = OrgStripeOnboardingStatus.NOT_CONNECTED;
        }
        if (chargesEnabled == null) {
            chargesEnabled = false;
        }
        if (payoutsEnabled == null) {
            payoutsEnabled = false;
        }
        if (detailsSubmitted == null) {
            detailsSubmitted = false;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
