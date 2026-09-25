package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_mfa_login_challenges", schema = "public")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthMfaLoginChallenge extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "auth_identity_id", nullable = false)
    private AuthIdentity authIdentity;

    @Column(name = "jti_hash", nullable = false, unique = true, length = 64)
    private String jtiHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;
}
