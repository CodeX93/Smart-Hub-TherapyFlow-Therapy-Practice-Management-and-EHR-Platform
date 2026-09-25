package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "Organisation-focused dashboard card response")
public class SuperAdminOrganisationDashboardResponse {

    @Schema(description = "Requested period code", example = "30d")
    private String period;

    @Schema(description = "Timezone used to compute the window", example = "UTC")
    private String timezone;

    @Schema(description = "Window start timestamp")
    private Instant from;

    @Schema(description = "Window end timestamp (exclusive)")
    private Instant to;

    @Schema(description = "Total organisations on the platform")
    private long totalOrganisations;

    @Schema(description = "Organisation counts by status bucket")
    private List<SuperAdminOrganisationStatusCountResponse> statusCounts;

    @Schema(description = "Top tenants for the window, sorted by active staff users")
    private List<SuperAdminTopTenantResponse> topTenants;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
