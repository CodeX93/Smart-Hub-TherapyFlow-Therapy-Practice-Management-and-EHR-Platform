package com.smart.therapy.flow.notification.repository;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.tenant.TenantScoped;
import com.smart.therapy.flow.notification.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Extended repository methods for Notification entity with client support.
 * 
 * HIPAA Compliance:
 * - All queries filter by is_deleted = false by default
 * - Client/User isolation enforced at query level
 * - Optimized indexes for performance
 */
@Repository
@TenantScoped
public interface NotificationRepositoryExtensions extends JpaRepository<Notification, Long> {

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

    // ========== User Notification Queries (Existing, for reference) ==========

    /**
     * Find all active notifications for a user
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isDeleted = :isDeleted ORDER BY n.createdAt DESC")
    List<Notification> findByUserAndIsDeletedOrderByCreatedAtDesc(
            @Param("user") User user,
            @Param("isDeleted") Boolean isDeleted
    );

    /**
     * Find notifications for a user by read status
     */
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.isRead = :isRead AND n.isDeleted = :isDeleted ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdAndIsReadAndIsDeletedOrderByCreatedAtDesc(
            @Param("user") User user,
            @Param("isRead") Boolean isRead,
            @Param("isDeleted") Boolean isDeleted
    );

    /**
     * Count unread notifications for a user
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = :isRead AND n.isDeleted = :isDeleted")
    long countByUserIdAndIsReadAndIsDeleted(
            @Param("user") User user,
            @Param("isRead") Boolean isRead,
            @Param("isDeleted") Boolean isDeleted
    );
}

