package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "MRR breakdown for dashboard")
public class SuperAdminMrrBreakdownChartResponse {

    @Schema(description = "Display currency", example = "USD")
    private String currency;

    @Schema(description = "MRR by plan")
    private List<PlanMrrItem> plans;

    @Schema(description = "Total MRR")
    private BigDecimal totalMrr;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Plan MRR row")
    public static class PlanMrrItem {
        @Schema(description = "Plan code", example = "ENTERPRISE")
        private String planCode;

        @Schema(description = "Plan name", example = "Enterprise")
        private String planName;

        @Schema(description = "MRR amount for this plan")
        private BigDecimal mrr;

        @Schema(description = "Percentage share of total MRR")
        private BigDecimal percentage;
    }
}
