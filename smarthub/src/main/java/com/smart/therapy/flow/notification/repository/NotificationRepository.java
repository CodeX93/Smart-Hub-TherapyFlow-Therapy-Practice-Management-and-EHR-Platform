package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@TenantScoped
public interface NotificationRepository extends JpaRepository<Notification, Long> {
    List<Notification> findByUserId(Long userId);
    List<Notification> findByIsRead(Boolean isRead);
    List<Notification> findByUserIdAndIsRead(Long userId, Boolean isRead);
    List<Notification> findByUserIdAndIsDeletedOrderByCreatedAtDesc(Long userId, Boolean isDeleted);
    List<Notification> findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(Long userId, Boolean isRead, Boolean isDeleted);
    List<Notification> findByIsDeletedOrderByCreatedAtDesc(Boolean isDeleted);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.user.id = :userId AND n.isDeleted = false ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.relatedEntityType = :entityType AND n.relatedEntityId = :entityId ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntity(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.relatedEntityType = :entityType AND n.relatedEntityId IN :entityIds ORDER BY n.createdAt DESC")
    List<Notification> findByRelatedEntityTypeAndEntityIds(@Param("entityType") String entityType, @Param("entityIds") List<Long> entityIds);

    @Query("SELECT n FROM Notification n LEFT JOIN FETCH n.user WHERE n.user.id = :userId")
    List<Notification> findByUserIdWithRelations(@Param("userId") Long userId);

    // ========== Client Notification Queries ==========

    /**
     * Find all active notifications for a client
     */
    @Query("SELECT n FROM Notification n WHERE n.client = :client AND n.isDeleted = :isDeleted ORDER BY n.createdAt DESC")
    List<Notification> findByClientAndIsDeletedOrderByCreatedAtDesc(
            @Param("client") Client client,
            @Param("isDeleted") Boolean isDeleted
    );

    /**
     * Find notifications for a client by read status
     */
    @Query("SELECT n FROM Notification n WHERE n.client = :client AND n.isRead = :isRead AND n.isDeleted = :isDeleted ORDER BY n.createdAt DESC")
    List<Notification> findByClientIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(
            @Param("client") Client client,
            @Param("isRead") Boolean isRead,
            @Param("isDeleted") Boolean isDeleted
    );

    /**
     * Count unread notifications for a client
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.client = :client AND n.isRead = :isRead AND n.isDeleted = :isDeleted")
    long countByClientIdAndIsReadAndIsDeleted(
            @Param("client") Client client,
            @Param("isRead") Boolean isRead,
            @Param("isDeleted") Boolean isDeleted
    );

    @Query(
            value = "SELECT n FROM Notification n WHERE n.client = :client AND n.isDeleted = :isDeleted",
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.client = :client AND n.isDeleted = :isDeleted")
    Page<Notification> findByClientAndIsDeleted(
            @Param("client") Client client,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);

    @Query(
            value = "SELECT n FROM Notification n WHERE n.client = :client AND n.isRead = :isRead AND n.isDeleted = :isDeleted",
            countQuery = "SELECT COUNT(n) FROM Notification n WHERE n.client = :client AND n.isRead = :isRead AND n.isDeleted = :isDeleted")
    Page<Notification> findByClientIdAndIsReadAndIsDeleted(
            @Param("client") Client client,
            @Param("isRead") Boolean isRead,
            @Param("isDeleted") Boolean isDeleted,
            Pageable pageable);
}




