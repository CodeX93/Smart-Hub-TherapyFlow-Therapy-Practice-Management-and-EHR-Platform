package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "Revenue overview response for the dashboard card")
public class SuperAdminRevenueOverviewResponse {

    @Schema(description = "Requested period code", example = "30d")
    private String period;

    @Schema(description = "Timezone used to compute the window", example = "UTC")
    private String timezone;

    @Schema(description = "Window start timestamp")
    private Instant from;

    @Schema(description = "Window end timestamp (exclusive)")
    private Instant to;

    @Schema(description = "Monthly recurring revenue in the window (USD)")
    private BigDecimal mrr;

    @Schema(description = "Annual recurring revenue (USD)")
    private BigDecimal arr;

    @Schema(description = "Churn rate percentage for the window")
    private BigDecimal churnRate;

    @Schema(description = "Revenue by plan for the window")
    private List<SuperAdminRevenueByPlanResponse> revenueByPlan;

    @Schema(description = "Upgrades in the window")
    private long upgrades;

    @Schema(description = "Downgrades in the window")
    private long downgrades;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
