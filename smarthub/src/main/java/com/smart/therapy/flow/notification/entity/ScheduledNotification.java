package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.notification.enums.EntityType;
import com.smart.therapy.flow.notification.enums.ScheduledNotificationStatus;
import com.smart.therapy.flow.session.entity.Session;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "scheduled_notifications", indexes = {
    @Index(name = "idx_scheduled_notification_status", columnList = "status"),
    @Index(name = "idx_scheduled_notification_execute_at", columnList = "execute_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ScheduledNotification extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ScheduledNotificationStatus status = ScheduledNotificationStatus.PENDING;

    @Column(name = "execute_at", nullable = false)
    private Instant executeAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trigger_id")
    private NotificationTrigger trigger;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session session;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type")
    private EntityType entityType;

    @Column(name = "entity_id")
    private Long entityId;

    // Plain text column; @Lob would make Hibernate stream it as a CLOB, which the
    // Postgres driver cannot do against a text column ("Unable to access lob stream").
    @Column(name = "entity_data", columnDefinition = "TEXT")
    private String entityData;
}
