package com.smart.therapy.flow.auth.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

@Entity
@Table(name = "auth_mfa_recovery_codes", schema = "public")
@AttributeOverrides({
        @AttributeOverride(name = "createdAt", column = @Column(name = "created_at", nullable = false, updatable = false)),
        @AttributeOverride(name = "updatedAt", column = @Column(name = "updated_at", nullable = false))
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class AuthMfaRecoveryCode extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "credential_id", nullable = false)
    private AuthMfaCredential credential;

    @Column(name = "code_hash", nullable = false, length = 255)
    private String codeHash;

    @Column(name = "used_at")
    private Instant usedAt;
}
