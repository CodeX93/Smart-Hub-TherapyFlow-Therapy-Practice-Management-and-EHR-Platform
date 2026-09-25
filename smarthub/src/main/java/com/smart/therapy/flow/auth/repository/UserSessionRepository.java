package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.UserSession;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    Optional<UserSession> findBySessionToken(String sessionToken);
    List<UserSession> findByUserId(Long userId);
    void deleteBySessionToken(String sessionToken);
}

