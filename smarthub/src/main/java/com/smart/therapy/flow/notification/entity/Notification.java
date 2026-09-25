package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationPriority;
import com.smart.therapy.flow.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Notification entity for both users (therapists/admins) and clients.
 * 
 * HIPAA Compliance:
 * - Soft delete support (is_deleted, deleted_at)
 * - Access control enforced at service layer
 * - Email tracking for audit trail
 * - Client/User isolation (exactly one recipient)
 */
@Entity
@Table(name = "notifications", indexes = {
    @Index(name = "idx_notification_user", columnList = "user_id"),
    @Index(name = "idx_notification_client", columnList = "client_id"),
    @Index(name = "idx_notification_is_read", columnList = "is_read"),
    @Index(name = "idx_notification_category", columnList = "category")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class Notification extends BaseEntity {

    // ========== Recipient (Either User OR Client, not both) ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // ========== Notification Content ==========
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    @Builder.Default
    private NotificationCategory category = NotificationCategory.SYSTEM;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "TEXT")
    private String data; // JSON string for additional data

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationPriority priority = NotificationPriority.MEDIUM;

    // ========== Read Status ==========
    
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(name = "read_at")
    private Instant readAt;

    // ========== Action Button ==========
    
    @Column(name = "action_url", length = 500)
    private String actionUrl; // URL to navigate when clicked

    @Column(name = "action_label", length = 100)
    private String actionLabel; // Button text for action

    // ========== Grouping & Batching ==========
    
    @Column(name = "grouping_key", length = 100)
    private String groupingKey; // For batching similar notifications

    @Column(name = "expires_at")
    private Instant expiresAt; // Auto-cleanup date

    // ========== Related Entity ==========
    
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType; // 'client', 'session', 'task', etc.

    @Column(name = "related_entity_id")
    private Long relatedEntityId; // ID of the related entity

    // ========== Email Tracking (Audit Trail) ==========
    
    @Column(name = "email_sent", nullable = false)
    @Builder.Default
    private Boolean emailSent = false;

    @Column(name = "email_sent_at")
    private Instant emailSentAt;

    @Column(name = "email_error", columnDefinition = "TEXT")
    private String emailError;

    // ========== Helper Methods ==========
    
    /**
     * Mark notification as read
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = Instant.now();
        }
    }

    
    /**
     * Check if notification is for a client
     */
    public boolean isClientNotification() {
        return this.client != null;
    }

    /**
     * Check if notification is for a user (therapist/admin)
     */
    public boolean isUserNotification() {
        return this.user != null;
    }

    /**
     * Get recipient ID (works for both user and client)
     */
    public Long getRecipientId() {
        return user != null ? user.getId() : (client != null ? client.getId() : null);
    }

    /**
     * Get recipient name (works for both user and client).
     * HIPAA: client recipients are identified by MRN, never display name.
     */
    public String getRecipientName() {
        if (user != null) {
            return user.getFullName();
        }
        if (client != null) {
            return client.getClientId() != null ? client.getClientId() : null;
        }
        return null;
    }
}

