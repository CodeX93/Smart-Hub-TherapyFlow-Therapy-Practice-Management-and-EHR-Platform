package com.smart.therapy.flow.notification.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class NotificationTriggerResponse {
    Long id;
    String name;
    String description;
    String eventType;
    com.smart.therapy.flow.notification.enums.EntityType entityType;
    String conditionRules;
    String recipientRules;
    String priority;
    Boolean isScheduled;
    Integer scheduleOffsetMinutes;
    Integer batchWindowMinutes;
    Integer maxBatchSize;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
}
