package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.auth.enums.MfaMethod;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_mfa_otp_challenges", schema = "public", indexes = {
        @Index(name = "idx_auth_mfa_otp_expiry", columnList = "auth_identity_id, expires_at, consumed_at")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_auth_mfa_otp_jti_purpose", columnNames = {"jti_hash", "purpose"})
})
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthMfaOtpChallenge extends BaseEntity {

    public enum Purpose {
        LOGIN,
        ENROLLMENT,
        SETTINGS
    }

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_identity_id", nullable = false)
    private AuthIdentity authIdentity;

    @Column(name = "jti_hash", nullable = false, length = 64)
    private String jtiHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private Purpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private MfaMethod channel;

    @Column(name = "code_hash", nullable = false)
    private String codeHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;
}
