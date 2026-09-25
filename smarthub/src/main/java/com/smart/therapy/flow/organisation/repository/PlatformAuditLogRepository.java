package com.smart.therapy.flow.organisation.repository;

import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PlatformAuditLogRepository extends JpaRepository<PlatformAuditLog, Long>, JpaSpecificationExecutor<PlatformAuditLog> {

    @Query(value = "SELECT resource_type, COUNT(*) " +
            "FROM public.platform_audit_logs " +
            "WHERE created_at >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "GROUP BY resource_type " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countRecentGlobalByResourceType(@Param("since") Instant since,
                                                   @Param("sourceMarker") String sourceMarker);

    @Query(value = "SELECT action, COUNT(*) " +
            "FROM public.platform_audit_logs " +
            "WHERE created_at >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "GROUP BY action " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countRecentGlobalByAction(@Param("since") Instant since,
                                             @Param("sourceMarker") String sourceMarker);

    @Query(value = "SELECT COUNT(*) " +
            "FROM public.platform_audit_logs " +
            "WHERE created_at >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "AND LOWER(action) LIKE '%error%'", nativeQuery = true)
    long countRecentGlobalErrors(@Param("since") Instant since,
                                 @Param("sourceMarker") String sourceMarker);
}
