package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthMfaRecoveryCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuthMfaRecoveryCodeRepository extends JpaRepository<AuthMfaRecoveryCode, Long> {
    List<AuthMfaRecoveryCode> findByCredentialIdAndUsedAtIsNull(Long credentialId);
    void deleteByCredentialId(Long credentialId);
}
