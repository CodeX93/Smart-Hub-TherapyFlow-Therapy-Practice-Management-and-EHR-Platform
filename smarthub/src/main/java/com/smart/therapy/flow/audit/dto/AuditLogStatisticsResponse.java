package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO for audit log statistics
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogStatisticsResponse {
    private Long totalActivities;
    private Long phiAccess;
    private Long highRiskEvents;
    private Long failedAttempts;
    private Long lowRiskEvents;
    private Long mediumRiskEvents;
    private Long criticalRiskEvents;
    private List<UserActivitySummary> userActivity;
}
