package com.smart.therapy.flow.notification.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationResponse {

    private Long id;
    private Long userId;
    private Long clientId;
    private com.smart.therapy.flow.notification.enums.NotificationType type;
    private com.smart.therapy.flow.notification.enums.NotificationCategory category;
    private String title;
    private String message;
    private String data;
    private String priority;
    private Boolean isRead;
    private Instant readAt;
    private String actionUrl;
    private String actionLabel;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Instant expiresAt;
    private Instant createdAt;
}

