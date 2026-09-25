package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditRiskDistributionResponse {
    private Long totalEvents;
    private Long lowRiskEvents;
    private Long mediumRiskEvents;
    private Long highRiskEvents;
    private Double lowRiskPercentage;
    private Double mediumRiskPercentage;
    private Double highRiskPercentage;
}
