package com.smart.therapy.flow.superadmin.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "platform_notification_triggers", schema = "public")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlatformNotificationTrigger {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Column(name = "entity_type", length = 50)
    private String entityType;

    @Column(name = "condition_rules", columnDefinition = "TEXT")
    private String conditionRules;

    @Column(name = "recipient_rules", columnDefinition = "TEXT")
    private String recipientRules;

    @Column(nullable = false, length = 20)
    private String priority;

    @Column(name = "delay_minutes", nullable = false)
    private Integer delayMinutes;

    @Column(name = "batch_window_minutes", nullable = false)
    private Integer batchWindowMinutes;

    @Column(name = "max_batch_size", nullable = false)
    private Integer maxBatchSize;

    @Column(name = "is_scheduled", nullable = false)
    private Boolean isScheduled;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
