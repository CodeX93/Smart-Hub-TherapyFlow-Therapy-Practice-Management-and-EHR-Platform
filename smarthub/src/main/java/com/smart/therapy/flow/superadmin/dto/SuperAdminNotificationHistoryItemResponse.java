package com.smart.therapy.flow.superadmin.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SuperAdminNotificationHistoryItemResponse {
    Long id;
    String jobType;
    String title;
    String message;
    String targetJson;
    String channel;
    String status;
    Instant scheduledAt;
    Instant sentAt;
    String errorMessage;
    Long createdByAuthId;
    Instant createdAt;
    Instant updatedAt;
    Boolean isRead;
    Instant readAt;
}
