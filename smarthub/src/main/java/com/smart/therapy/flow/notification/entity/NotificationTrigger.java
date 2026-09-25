package com.smart.therapy.flow.notification.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.notification.enums.EntityType;
import com.smart.therapy.flow.notification.enums.EntityTypeConverter;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "notification_triggers")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class NotificationTrigger extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    @Convert(converter = EntityTypeConverter.class)
    @Column(name = "entity_type")
    private EntityType entityType; // 'client', 'session', 'task'

    @Column(name = "condition_rules", columnDefinition = "TEXT")
    private String conditionRules; // JSON string for flexible conditions

    @Column(name = "recipient_rules", columnDefinition = "TEXT")
    private String recipientRules; // JSON string for who gets notified

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id")
    private NotificationTemplate template;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String priority = "medium";

    @Column(name = "delay_minutes")
    @Builder.Default
    private Integer delayMinutes = 0; // Delay before sending

    @Column(name = "batch_window_minutes")
    @Builder.Default
    private Integer batchWindowMinutes = 5; // Grouping window

    @Column(name = "max_batch_size")
    @Builder.Default
    private Integer maxBatchSize = 10;

    @Column(name = "is_scheduled", nullable = false)
    @Builder.Default
    private Boolean isScheduled = false; // True for 24hr reminders that need scheduling

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
