package com.smart.therapy.flow.notification.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStatsResponse {
    private Long total;
    private Long unread;
}

