package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuthSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface AuthSessionRepository extends JpaRepository<AuthSession, Long> {

    Optional<AuthSession> findByJwtId(String jwtId);

    /** Activity refresh and revocation checks share a short transaction per session. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM AuthSession s WHERE s.jwtId = :jwtId")
    Optional<AuthSession> findByJwtIdForUpdate(@Param("jwtId") String jwtId);

    List<AuthSession> findByAuthIdentityId(Long authId);

    List<AuthSession> findByImpersonationSessionId(Long impersonationSessionId);

    @Modifying
    @Query("DELETE FROM AuthSession s WHERE s.expiresAt < :before")
    int deleteExpiredBefore(@Param("before") Instant before);
}
