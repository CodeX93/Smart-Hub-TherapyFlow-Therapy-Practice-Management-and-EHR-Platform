package com.smart.therapy.flow.notification.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationRequest {

    /**
     * Target user (nullable if sending to client)
     */
    private Long userId;

    /**
     * Target client (nullable if sending to user)
     */
    private Long clientId;

    @NotNull
    private com.smart.therapy.flow.notification.enums.NotificationType type;

    private com.smart.therapy.flow.notification.enums.NotificationCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String message;

    /**
     * Optional JSON payload string
     */
    private String data;

    private String priority;
    private String actionUrl;
    private String actionLabel;
    private String relatedEntityType;
    private Long relatedEntityId;
    private java.time.Instant expiresAt;
}

