package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthKnownDevice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthKnownDeviceRepository extends JpaRepository<AuthKnownDevice, Long> {

    Optional<AuthKnownDevice> findByAuthIdentityIdAndFingerprintHash(Long authIdentityId, String fingerprintHash);

    Optional<AuthKnownDevice> findByAuthIdentityIdAndTrustTokenHash(Long authIdentityId, String trustTokenHash);

    Optional<AuthKnownDevice> findByIdAndAuthIdentityId(Long id, Long authIdentityId);

    List<AuthKnownDevice> findByAuthIdentityIdOrderByLastSeenAtDesc(Long authIdentityId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update AuthKnownDevice d
               set d.trusted = false,
                   d.trustRevokedAt = :revokedAt,
                   d.trustTokenHash = null,
                   d.updatedAt = :revokedAt
             where d.authIdentity.id = :authId
               and d.trusted = true
               and d.trustRevokedAt is null
            """)
    int revokeAllTrustedForAuthId(@Param("authId") Long authId, @Param("revokedAt") Instant revokedAt);
}
