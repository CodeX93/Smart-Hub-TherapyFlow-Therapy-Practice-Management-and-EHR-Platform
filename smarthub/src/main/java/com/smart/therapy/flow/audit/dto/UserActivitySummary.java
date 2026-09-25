package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * DTO for user activity summary
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserActivitySummary {
    private String username;
    private Long activityCount;
    private Instant lastActivity;
}
