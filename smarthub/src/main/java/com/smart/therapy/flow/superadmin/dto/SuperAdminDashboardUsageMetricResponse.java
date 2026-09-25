package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Usage metric row for dashboard summary")
public class SuperAdminDashboardUsageMetricResponse {
    @Schema(description = "Feature code", example = "SESSIONS_PER_MONTH")
    private String featureKey;

    @Schema(description = "Total usage in period")
    private long usageCount;
}
