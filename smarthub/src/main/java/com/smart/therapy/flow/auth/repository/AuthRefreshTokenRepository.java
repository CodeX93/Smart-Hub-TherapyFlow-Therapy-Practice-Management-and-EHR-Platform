package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthRefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthRefreshTokenRepository extends JpaRepository<AuthRefreshToken, Long> {

    Optional<AuthRefreshToken> findByTokenHash(String tokenHash);

    List<AuthRefreshToken> findByFamilyId(String familyId);

    List<AuthRefreshToken> findByAuthIdentityIdAndRevokedFalse(Long authId);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.revoked = true, t.revokedAt = :now "
            + "WHERE t.familyId = :familyId AND t.revoked = false")
    int revokeFamily(@Param("familyId") String familyId, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.reuseDetected = true WHERE t.id = :id")
    int markReuseDetected(@Param("id") Long id);

    @Modifying
    @Query("UPDATE AuthRefreshToken t SET t.revoked = true, t.revokedAt = :now "
            + "WHERE t.authIdentity.id = :authId AND t.revoked = false")
    int revokeAllForAuthId(@Param("authId") Long authId, @Param("now") Instant now);

    @Modifying
    @Query("DELETE FROM AuthRefreshToken t WHERE t.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
