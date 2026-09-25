package com.smart.therapy.flow.session.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.session.entity.SessionIntegration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@TenantScoped
public interface SessionIntegrationRepository extends JpaRepository<SessionIntegration, Long> {
    Optional<SessionIntegration> findBySessionIdAndProvider(Long sessionId, String provider);

    @Query("SELECT si FROM SessionIntegration si WHERE si.session.id IN :sessionIds")
    List<SessionIntegration> findBySessionIdIn(@Param("sessionIds") Collection<Long> sessionIds);

    @Query("""
            SELECT si.session.id, si.joinUrl, si.provider
            FROM SessionIntegration si
            WHERE si.session.id IN :sessionIds
              AND LOWER(si.provider) = 'zoom'
            """)
    List<Object[]> findZoomJoinBySessionIds(@Param("sessionIds") Collection<Long> sessionIds);
}
