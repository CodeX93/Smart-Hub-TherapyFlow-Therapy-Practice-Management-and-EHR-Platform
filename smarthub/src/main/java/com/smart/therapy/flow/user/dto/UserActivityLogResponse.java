package com.smart.therapy.flow.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class UserActivityLogResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String activityType; // Updated to match entity field name
    private String description; // Updated to match entity field name
    private String ipAddress;
    private String userAgent;
    private Instant createdAt;
}

