package com.smart.therapy.flow.notification.dto;

import com.smart.therapy.flow.notification.enums.NotificationCategory;
import com.smart.therapy.flow.notification.enums.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationBroadcastRequest {

    @NotNull
    private NotificationTargetType targetType;

    @NotNull
    private NotificationType type;

    private NotificationCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    private String data;
    private String priority;
    private String actionUrl;
    private String actionLabel;
    private String relatedEntityType;
    private Long relatedEntityId;
    private Instant expiresAt;
}

