package com.smart.therapy.flow.notification.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class NotificationTemplateResponse {
    Long id;
    String name;
    String type;
    String eventType;
    String subject;
    String bodyTemplate;
    Boolean isSystem;
    Boolean isActive;
    Instant createdAt;
    Instant updatedAt;
}
