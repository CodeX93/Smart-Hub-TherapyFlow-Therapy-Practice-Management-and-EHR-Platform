package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthMfaCredential;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface AuthMfaCredentialRepository extends JpaRepository<AuthMfaCredential, Long> {
    Optional<AuthMfaCredential> findByAuthIdentityId(Long authIdentityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from AuthMfaCredential c where c.authIdentity.id = :authId")
    Optional<AuthMfaCredential> findByAuthIdentityIdForUpdate(@Param("authId") Long authId);
}
