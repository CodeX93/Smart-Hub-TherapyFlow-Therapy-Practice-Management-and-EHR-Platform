package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.organisation.entity.Organisation;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

/**
 * Unified identity: credentials + login directory in {@code public.auth_identities}.
 * Tenant STAFF/CLIENT identities are scoped by {@code organisation_id}.
 * Uniqueness is DB-enforced per org on {@code normalised_email} / {@code normalised_username}.
 * {@code loginIdentifier} remains a display/JWT alias (prefer username for staff, email for clients).
 */
@Entity
@AttributeOverrides({
    @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
    @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Table(name = "auth_identities", schema = "public", indexes = {
    @Index(name = "idx_auth_normalised_login_identifier", columnList = "normalised_login_identifier"),
    @Index(name = "idx_auth_identities_normalised_email", columnList = "normalised_email"),
    @Index(name = "idx_auth_identities_normalised_username", columnList = "normalised_username"),
    @Index(name = "idx_auth_identity_type", columnList = "identity_type"),
    @Index(name = "idx_auth_locked", columnList = "account_locked, locked_until")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@EqualsAndHashCode(callSuper = true, exclude = "roles")
public class AuthIdentity extends BaseEntity {

    /** Required for tenant STAFF/CLIENT; null only for platform identities. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organisation_id")
    private Organisation organisation;

    /** Display / JWT alias — prefer username (staff) or email (client). */
    @Column(name = "login_identifier", nullable = false, length = 255)
    private String loginIdentifier;

    @Column(name = "normalised_login_identifier", nullable = false, length = 255)
    private String normalisedLoginIdentifier;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "normalised_email", length = 255)
    private String normalisedEmail;

    /** Staff login username; null for CLIENT. */
    @Column(name = "username", length = 255)
    private String username;

    @Column(name = "normalised_username", length = 255)
    private String normalisedUsername;

    @Column(name = "full_name", length = 150)
    private String fullName;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "login_identifier_changed_at")
    private Instant loginIdentifierChangedAt;

    /** Null for SSO-only identities (no password login). */
    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_type", nullable = false, length = 20)
    private IdentityType identityType;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false, length = 30)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    /** Unique id from SSO provider; null for LOCAL. */
    @Column(name = "provider_user_id", length = 255)
    private String providerUserId;

    @Column(name = "sso_enabled", nullable = false)
    @Builder.Default
    private Boolean ssoEnabled = false;

    @Column(name = "email_verified")
    private Boolean emailVerified;

    @Column(name = "email_verification_token", length = 255)
    private String emailVerificationToken;

    @Column(name = "email_verification_expiry")
    private Instant emailVerificationExpiry;

    @Column(name = "password_reset_token", length = 255)
    private String passwordResetToken;

    @Column(name = "password_reset_expiry")
    private Instant passwordResetExpiry;

    @Column(name = "password_changed_at")
    private Instant passwordChangedAt;

    @Column(name = "must_change_password", nullable = false)
    @Builder.Default
    private Boolean mustChangePassword = false;

    @Column(name = "last_password_change_by")
    private Long lastPasswordChangeBy;

    @Column(name = "failed_login_attempts", nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;

    @Column(name = "last_failed_login")
    private Instant lastFailedLogin;

    @Column(name = "last_successful_login")
    private Instant lastSuccessfulLogin;

    @Column(name = "account_locked", nullable = false)
    @Builder.Default
    private Boolean accountLocked = false;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "locked_reason", columnDefinition = "TEXT")
    private String lockedReason;

    @OneToMany(mappedBy = "authIdentity", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<AuthIdentityRole> roles = new HashSet<>();
}
