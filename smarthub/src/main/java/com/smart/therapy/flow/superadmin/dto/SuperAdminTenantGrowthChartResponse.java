package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "Tenant growth chart response")
public class SuperAdminTenantGrowthChartResponse {

    @Schema(description = "Requested range", example = "7w")
    private String range;

    @Schema(description = "Timezone used for bucket generation", example = "UTC")
    private String timezone;

    @Schema(description = "Range start")
    private Instant from;

    @Schema(description = "Range end")
    private Instant to;

    @Schema(description = "Chart points")
    private List<Point> points;

    @Schema(description = "Total newly created tenants in range")
    private long totalNewTenants;

    @Schema(description = "Total churned tenants in range")
    private long totalChurnedTenants;

    @Schema(description = "Net tenant growth in range")
    private long netGrowth;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Growth chart point")
    public static class Point {
        @Schema(description = "Bucket label", example = "2026-04-22")
        private String label;

        @Schema(description = "Bucket start")
        private Instant from;

        @Schema(description = "Bucket end")
        private Instant to;

        @Schema(description = "New tenants in this bucket")
        private long newTenants;

        @Schema(description = "Churned tenants in this bucket")
        private long churnedTenants;

        @Schema(description = "Net growth in this bucket")
        private long netGrowth;
    }
}
