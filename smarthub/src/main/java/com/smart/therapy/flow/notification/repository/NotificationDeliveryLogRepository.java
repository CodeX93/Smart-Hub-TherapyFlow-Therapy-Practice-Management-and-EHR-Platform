package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.NotificationDeliveryLog;
import com.smart.therapy.flow.notification.enums.NotificationChannel;
import com.smart.therapy.flow.notification.enums.NotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
@TenantScoped
public interface NotificationDeliveryLogRepository extends JpaRepository<NotificationDeliveryLog, Long> {

    @Query("""
            SELECT d FROM NotificationDeliveryLog d
            JOIN FETCH d.notification n
            WHERE d.channel = :channel
              AND d.status IN :statuses
              AND n.client = :client
              AND (n.isDeleted = false OR n.isDeleted IS NULL)
            ORDER BY COALESCE(d.deliveredAt, d.sentAt, d.createdAt) DESC
            """)
    List<NotificationDeliveryLog> findSuccessfulByClientAndChannel(
            @Param("client") Client client,
            @Param("channel") NotificationChannel channel,
            @Param("statuses") Collection<NotificationStatus> statuses);

    @Query("""
            SELECT d FROM NotificationDeliveryLog d
            JOIN FETCH d.notification n
            WHERE d.channel = :channel
              AND d.status IN :statuses
              AND LOWER(n.relatedEntityType) = LOWER(:entityType)
              AND n.relatedEntityId = :entityId
              AND (n.isDeleted = false OR n.isDeleted IS NULL)
            ORDER BY COALESCE(d.deliveredAt, d.sentAt, d.createdAt) DESC
            """)
    List<NotificationDeliveryLog> findSuccessfulByRelatedEntity(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("channel") NotificationChannel channel,
            @Param("statuses") Collection<NotificationStatus> statuses);

    @Query("""
            SELECT d FROM NotificationDeliveryLog d
            JOIN FETCH d.notification n
            WHERE d.channel = :channel
              AND d.status IN :statuses
              AND LOWER(n.relatedEntityType) = LOWER(:entityType)
              AND n.relatedEntityId IN :entityIds
              AND (n.isDeleted = false OR n.isDeleted IS NULL)
            ORDER BY COALESCE(d.deliveredAt, d.sentAt, d.createdAt) DESC
            """)
    List<NotificationDeliveryLog> findSuccessfulByRelatedEntities(
            @Param("entityType") String entityType,
            @Param("entityIds") Collection<Long> entityIds,
            @Param("channel") NotificationChannel channel,
            @Param("statuses") Collection<NotificationStatus> statuses);
}
