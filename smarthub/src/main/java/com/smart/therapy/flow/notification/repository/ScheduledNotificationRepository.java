package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.ScheduledNotification;
import com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
@TenantScoped
public interface ScheduledNotificationRepository extends JpaRepository<ScheduledNotification, Long> {
    List<ScheduledNotification> findByStatusAndExecuteAtBefore(ScheduledNotificationStatus status, Instant executeAt);
    
    // Check for duplicate scheduled notifications (matches ClientHubAI duplicate prevention)
    @Query("SELECT COUNT(s) > 0 FROM ScheduledNotification s WHERE " +
           "(s.session.id = :sessionId OR (:sessionId IS NULL AND s.session IS NULL)) " +
           "AND s.trigger.id = :triggerId AND s.status = :status")
    boolean existsBySessionAndTriggerAndStatus(@Param("sessionId") Long sessionId, 
                                               @Param("triggerId") Long triggerId, 
                                               @Param("status") ScheduledNotificationStatus status);
}





