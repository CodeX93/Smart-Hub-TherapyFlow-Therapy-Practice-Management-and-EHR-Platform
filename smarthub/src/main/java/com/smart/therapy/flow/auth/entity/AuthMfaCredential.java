package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_mfa_credentials", schema = "public")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthMfaCredential extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_identity_id", nullable = false, unique = true)
    private AuthIdentity authIdentity;

    @Column(name = "encrypted_secret", columnDefinition = "TEXT")
    private String encryptedSecret;

    @Column(name = "encrypted_pending_secret", columnDefinition = "TEXT")
    private String encryptedPendingSecret;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = false;

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    @Column(name = "last_accepted_time_step")
    private Long lastAcceptedTimeStep;

    @Column(name = "failed_attempts", nullable = false)
    @Builder.Default
    private Integer failedAttempts = 0;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Enumerated(EnumType.STRING)
    @Column(name = "mfa_method", nullable = false, length = 20)
    @Builder.Default
    private MfaMethod mfaMethod = MfaMethod.TOTP;

    @Column(name = "phone_e164", length = 20)
    private String phoneE164;

    @Column(name = "totp_enabled", nullable = false)
    @Builder.Default
    private Boolean totpEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "pending_mfa_method", length = 20)
    private MfaMethod pendingMfaMethod;

    @Column(name = "pending_phone_e164", length = 20)
    private String pendingPhoneE164;
}
