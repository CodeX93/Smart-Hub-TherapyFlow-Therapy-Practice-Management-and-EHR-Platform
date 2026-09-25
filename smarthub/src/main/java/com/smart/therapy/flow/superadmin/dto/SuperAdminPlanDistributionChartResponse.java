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
@Schema(description = "Plan distribution chart response")
public class SuperAdminPlanDistributionChartResponse {

    @Schema(description = "Total tenants counted in distribution")
    private long totalTenants;

    @Schema(description = "Plan tier distribution rows")
    private List<PlanDistributionItem> plans;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Plan distribution row")
    public static class PlanDistributionItem {
        @Schema(description = "Plan code", example = "ENTERPRISE")
        private String planCode;

        @Schema(description = "Plan name", example = "Enterprise")
        private String planName;

        @Schema(description = "Tenant count in this plan")
        private long tenants;

        @Schema(description = "Percentage share of total tenants")
        private BigDecimal percentage;
    }
}
