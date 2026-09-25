package com.smart.therapy.flow.client.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.client.entity.ClientPortalSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface ClientPortalSessionRepository extends JpaRepository<ClientPortalSession, Long> {
    Optional<ClientPortalSession> findBySessionToken(String sessionToken);
    List<ClientPortalSession> findByClientId(Long clientId);
}





