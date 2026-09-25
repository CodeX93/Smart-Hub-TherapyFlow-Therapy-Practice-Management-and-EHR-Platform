package com.smart.therapy.flow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferenceRequest {
    private com.smart.therapy.flow.notification.enums.NotificationType notificationType;
    private Boolean emailEnabled;
    private Boolean smsEnabled;
    private Boolean pushEnabled;
    private Boolean inAppEnabled;
    private com.smart.therapy.flow.notification.enums.NotificationTiming timing;
    private java.time.Instant quietHoursStart;
    private java.time.Instant quietHoursEnd;
    private Boolean weekendsEnabled;
}

