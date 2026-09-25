package com.smart.therapy.flow.auth.repository;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@TenantScoped
public interface AuditLogRepository extends JpaRepository<AuditLog, Long>, JpaSpecificationExecutor<AuditLog> {
    List<AuditLog> findByUserId(Long userId);

    List<AuditLog> findByAction(String action);

    /**
     * Find top active users with limit
     * Returns username, activity count, and last activity timestamp
     */
    @Query(value = "SELECT username, COUNT(*) as activity_count, MAX(timestamp) as last_activity " +
            "FROM audit_logs " +
            "WHERE username IS NOT NULL " +
            "GROUP BY username " +
            "ORDER BY activity_count DESC", nativeQuery = true)
    List<Object[]> findTopActiveUsers(org.springframework.data.domain.Pageable pageable);

    @Query("SELECT COUNT(a) > 0 FROM AuditLog a " +
            "WHERE a.timestamp >= :since " +
            "AND a.username = :username " +
            "AND a.action = :action " +
            "AND ((a.resourceType IS NULL AND :resourceType IS NULL) OR a.resourceType = :resourceType) " +
            "AND ((a.resourceId IS NULL AND :resourceId IS NULL) OR a.resourceId = :resourceId) " +
            "AND a.result = :result " +
            "AND a.ipAddress = :ipAddress " +
            "AND a.details LIKE %:sourceMarker%")
    boolean existsRecentGlobalDuplicate(@Param("since") Instant since,
                                        @Param("username") String username,
                                        @Param("action") String action,
                                        @Param("resourceType") String resourceType,
                                        @Param("resourceId") String resourceId,
                                        @Param("result") String result,
                                        @Param("ipAddress") String ipAddress,
                                        @Param("sourceMarker") String sourceMarker);

    @Query(value = "SELECT resource_type, COUNT(*) " +
            "FROM audit_logs " +
            "WHERE timestamp >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "GROUP BY resource_type " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countRecentGlobalByResourceType(@Param("since") Instant since,
                                                   @Param("sourceMarker") String sourceMarker);

    @Query(value = "SELECT action, COUNT(*) " +
            "FROM audit_logs " +
            "WHERE timestamp >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "GROUP BY action " +
            "ORDER BY COUNT(*) DESC", nativeQuery = true)
    List<Object[]> countRecentGlobalByAction(@Param("since") Instant since,
                                             @Param("sourceMarker") String sourceMarker);

    @Query(value = "SELECT COUNT(*) " +
            "FROM audit_logs " +
            "WHERE timestamp >= :since " +
            "AND details LIKE CONCAT('%', :sourceMarker, '%') " +
            "AND LOWER(result) IN ('failure','failed','blocked')", nativeQuery = true)
    long countRecentGlobalErrors(@Param("since") Instant since,
                                 @Param("sourceMarker") String sourceMarker);
}
