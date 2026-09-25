package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.notification.enums.NotificationTiming;
import com.smart.therapy.flow.notification.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * Notification preferences for both users (therapists/admins) and clients.
 * 
 * HIPAA Compliance:
 * - Opt-in model (SMS disabled by default)
 * - Quiet hours support
 * - Per-trigger-type granularity
 */
@Entity
@Table(name = "notification_preferences", indexes = {
    @Index(name = "idx_notification_pref_user", columnList = "user_id"),
    @Index(name = "idx_notification_pref_client", columnList = "client_id"),
    @Index(name = "idx_notification_pref_type", columnList = "notification_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationPreference extends BaseEntity {

    // ========== Owner (Either User OR Client, not both) ==========
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    // ========== Preference Settings ==========

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "email_enabled", nullable = false)
    @Builder.Default
    private Boolean emailEnabled = false;

    @Column(name = "sms_enabled", nullable = false)
    @Builder.Default
    private Boolean smsEnabled = false;

    @Column(name = "push_enabled", nullable = false)
    @Builder.Default
    private Boolean pushEnabled = false;

    @Column(name = "in_app_enabled", nullable = false)
    @Builder.Default
    private Boolean inAppEnabled = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private NotificationTiming timing = NotificationTiming.IMMEDIATE;


    // ========== Quiet Hours ==========
    
    @Column(name = "quiet_hours_start")
    private Instant quietHoursStart; // '22:00:00'

    @Column(name = "quiet_hours_end")
    private Instant quietHoursEnd; // '08:00:00'

    @Column(name = "weekends_enabled", nullable = false)
    @Builder.Default
    private Boolean weekendsEnabled = true;

    // ========== Helper Methods ==========
    
    /**
     * Check if this preference is for a client
     */
    public boolean isClientPreference() {
        return this.client != null;
    }

    /**
     * Check if this preference is for a user
     */
    public boolean isUserPreference() {
        return this.user != null;
    }

    /**
     * Get owner ID (works for both user and client)
     */
    public Long getOwnerId() {
        return user != null ? user.getId() : (client != null ? client.getId() : null);
    }

    /**
     * Check if any channel is enabled
     */
    public boolean hasEnabledChannel() {
        return Boolean.TRUE.equals(inAppEnabled) 
            || Boolean.TRUE.equals(emailEnabled)
            || Boolean.TRUE.equals(smsEnabled)
            || Boolean.TRUE.equals(pushEnabled);
    }

    /**
     * Check if notifications are completely disabled
     */
    public boolean isCompletelyDisabled() {
        return !hasEnabledChannel();
    }
}


