package com.smart.therapy.flow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceResponse {
    private Long id;
    private Long userId;
    private Long clientId;
    private com.smart.therapy.flow.notification.enums.NotificationType notificationType;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private com.smart.therapy.flow.notification.enums.NotificationTiming timing;
    private java.time.Instant quietHoursStart;
    private java.time.Instant quietHoursEnd;
    private Boolean weekendsEnabled;
    private Instant createdAt;
    private Instant updatedAt;
}

