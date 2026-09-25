package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditDashboardResponse {
    private LocalDate startDate;
    private LocalDate endDate;
    private String period;

    private Long totalActivities;
    private Long phiAccessEvents;
    private Long highRiskEvents;
    private Long failedAttempts;

    private Long lowRiskEvents;
    private Long mediumRiskEvents;
    private Long criticalRiskEvents;

    private List<UserActivitySummary> userActivitySummary;
    private AuditRiskDistributionResponse riskDistribution;
    private List<AuditLogResponse> logs;
    private AuditPageMetaResponse pagination;
}
