package com.smart.therapy.flow.superadmin.repository;

import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformImpersonationSessionRepository extends JpaRepository<PlatformImpersonationSession, Long> {
    List<PlatformImpersonationSession> findTop200ByOrderByCreatedAtDesc();

    List<PlatformImpersonationSession> findByStatusAndExpiresAtBefore(String status, Instant before);

    Optional<PlatformImpersonationSession> findByTokenPrefix(String tokenPrefix);
}
