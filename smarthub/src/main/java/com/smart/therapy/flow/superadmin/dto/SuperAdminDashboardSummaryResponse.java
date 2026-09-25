package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "Super admin dashboard summary response")
public class SuperAdminDashboardSummaryResponse {

    @Schema(description = "KPI aggregates for the requested period")
    private SuperAdminKpiResponse kpis;

    @Schema(description = "Count of delinquent organisations in the period")
    private long delinquentOrganisations;

    @Schema(description = "Count of past-due or failed invoices in the period")
    private long delinquentInvoices;

    @Schema(description = "Total organisations included in this response scope")
    private long totalOrganisations;

    @Schema(description = "Usage totals grouped by feature key")
    private List<SuperAdminDashboardUsageMetricResponse> usageMetrics;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
