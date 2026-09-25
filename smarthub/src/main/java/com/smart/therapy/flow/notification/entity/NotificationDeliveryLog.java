package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.notification.enums.NotificationChannel;
import com.smart.therapy.flow.notification.enums.NotificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

/**
 * Entity tracking notification delivery across different channels.
 * Provides audit trail for HIPAA compliance.
 */
@Entity
@Table(name = "notification_delivery_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDeliveryLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;
    
    // ========== Delivery Channel ==========
    
    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 20)
    private NotificationChannel channel;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    
    // ========== Recipient Details ==========
    
    @Column(name = "recipient_email")
    private String recipientEmail;
    
    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;
    
    @Column(name = "recipient_device_token", columnDefinition = "TEXT")
    private String recipientDeviceToken;
    
    // ========== External Service Tracking ==========
    
    @Column(name = "external_id")
    private String externalId; // Message ID from provider
    
    @Column(name = "provider", length = 50)
    private String provider; // AWS SES, FCM, Twilio, etc.
    
    // ========== Delivery Timestamps ==========
    
    @Column(name = "sent_at")
    private Instant sentAt;
    
    @Column(name = "delivered_at")
    private Instant deliveredAt;
    
    @Column(name = "failed_at")
    private Instant failedAt;
    
    // ========== Error Tracking ==========
    
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;
    
    @Column(name = "error_code", length = 50)
    private String errorCode;
    
    // ========== Retry Logic ==========
    
    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;
    
    @Column(name = "max_retries", nullable = false)
    @Builder.Default
    private Integer maxRetries = 3;
    
    @Column(name = "next_retry_at")
    private Instant nextRetryAt;
    
    // ========== Metadata ==========
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    // ========== Helper Methods ==========
    
    /**
     * Mark delivery as sent
     */
    public void markAsSent(String externalId) {
        this.sentAt = Instant.now();
        this.status = NotificationStatus.SENT;
        this.externalId = externalId;
    }
    
    /**
     * Mark delivery as delivered
     */
    public void markAsDelivered() {
        this.deliveredAt = Instant.now();
        this.status = NotificationStatus.DELIVERED;
    }
    
    /**
     * Mark delivery as failed
     */
    public void markAsFailed(String errorMessage, String errorCode) {
        this.failedAt = Instant.now();
        this.status = NotificationStatus.FAILED;
        this.errorMessage = errorMessage;
        this.errorCode = errorCode;
        this.retryCount++;
        
        if (this.retryCount >= this.maxRetries) {
            this.status = NotificationStatus.FAILED_PERMANENTLY;
            this.nextRetryAt = null;
        } else {
            // Exponential backoff: 5min, 15min, 45min
            long delayMinutes = (long) (5 * Math.pow(3, this.retryCount - 1));
            this.nextRetryAt = Instant.now().plusSeconds(delayMinutes * 60);
        }
    }
    
    /**
     * Check if retry should be attempted
     */
    public boolean shouldRetry() {
        return status == NotificationStatus.FAILED 
            && retryCount < maxRetries
            && nextRetryAt != null
            && Instant.now().isAfter(nextRetryAt);
    }
}

