package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminKpiResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardKpiCardsResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardTierAliasesReplaceRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardTierAliasesResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminMrrBreakdownChartResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminPlanDistributionChartResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminOrganisationDashboardResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDashboardSummaryResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminRevenueOverviewResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSystemHealthPanelResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminTenantGrowthChartResponse;
import com.smart.therapy.flow.superadmin.service.SuperAdminBillingService;
import com.smart.therapy.flow.superadmin.service.SuperAdminDashboardOverviewService;
import com.smart.therapy.flow.superadmin.service.SuperAdminKpiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.smart.therapy.flow.common.security.AuthPrincipal;

@RestController
@RequestMapping("/api/v1/super-admin")
@Tag(name = "Super Admin Metrics", description = "Global KPI aggregates for platform dashboard")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
public class SuperAdminMetricsController {

    private final SuperAdminKpiService kpiService;
    private final SuperAdminBillingService billingService;
    private final SuperAdminDashboardOverviewService dashboardOverviewService;

    @GetMapping("/metrics/kpis")
    @Operation(summary = "Get global KPI aggregates", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminKpiResponse> getKpis(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String timezone
    ) {
        boolean lagged = kpiService.isReplicaLagged();
        SuperAdminKpiResponse body = kpiService.getKpis(period, orgId, timezone);
        return ResponseEntity.ok()
                .header("X-Data-Lag", Boolean.toString(lagged))
                .body(body);
    }

    @GetMapping("/metrics/summary")
    @Operation(summary = "Get global dashboard summary", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminDashboardSummaryResponse> getSummary(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String timezone
    ) {
        boolean lagged = kpiService.isReplicaLagged();
        SuperAdminDashboardSummaryResponse body = kpiService.getDashboardSummary(period, orgId, timezone);
        return ResponseEntity.ok()
                .header("X-Data-Lag", Boolean.toString(lagged))
                .body(body);
    }

    @GetMapping("/dashboard")
    @Operation(summary = "Get combined dashboard response", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminDashboardSummaryResponse> getDashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) Long orgId,
            @RequestParam(required = false) String timezone
    ) {
        boolean lagged = kpiService.isReplicaLagged();
        SuperAdminDashboardSummaryResponse body = kpiService.getDashboardSummary(period, orgId, timezone);
        return ResponseEntity.ok()
                .header("X-Data-Lag", Boolean.toString(lagged))
                .body(body);
    }

    @GetMapping("/dashboard/organisations")
    @Operation(summary = "Get organisation dashboard card response", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminOrganisationDashboardResponse> getOrganisationDashboard(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String timezone
    ) {
        boolean lagged = kpiService.isReplicaLagged();
        SuperAdminOrganisationDashboardResponse body = kpiService.getOrganisationDashboard(period, timezone);
        return ResponseEntity.ok()
                .header("X-Data-Lag", Boolean.toString(lagged))
                .body(body);
    }

    @GetMapping("/dashboard/revenue-overview")
    @Operation(summary = "Get revenue overview card response", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminRevenueOverviewResponse> getRevenueOverview(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String timezone
    ) {
        boolean lagged = kpiService.isReplicaLagged();
        SuperAdminRevenueOverviewResponse body = billingService.getRevenueOverview(period, timezone);
        return ResponseEntity.ok()
                .header("X-Data-Lag", Boolean.toString(lagged))
                .body(body);
    }

    @GetMapping("/dashboard/kpis")
    @Operation(summary = "Get super admin dashboard KPI cards", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminDashboardKpiCardsResponse> getDashboardKpis(
            @RequestParam(required = false) String timezone
    ) {
        SuperAdminDashboardKpiCardsResponse body = dashboardOverviewService.getKpiCards(timezone);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/dashboard/tenant-growth")
    @Operation(summary = "Get tenant growth chart data", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminTenantGrowthChartResponse> getTenantGrowth(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String timezone
    ) {
        SuperAdminTenantGrowthChartResponse body = dashboardOverviewService.getTenantGrowth(range, timezone);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/dashboard/plan-distribution")
    @Operation(summary = "Get plan distribution breakdown", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminPlanDistributionChartResponse> getPlanDistribution() {
        SuperAdminPlanDistributionChartResponse body = dashboardOverviewService.getPlanDistribution();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/dashboard/mrr-breakdown")
    @Operation(summary = "Get MRR breakdown by plan tier", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminMrrBreakdownChartResponse> getMrrBreakdown() {
        SuperAdminMrrBreakdownChartResponse body = dashboardOverviewService.getMrrBreakdown();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/dashboard/system-health")
    @Operation(summary = "Get system health panel data", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminSystemHealthPanelResponse> getSystemHealthPanel() {
        SuperAdminSystemHealthPanelResponse body = dashboardOverviewService.getSystemHealthPanel();
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }

    @GetMapping("/dashboard/config/tier-aliases")
    @Operation(summary = "Get dashboard tier alias configuration", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminDashboardTierAliasesResponse> getTierAliases() {
        SuperAdminDashboardTierAliasesResponse body = dashboardOverviewService.getTierAliases();
        return ResponseEntity.ok(body);
    }

    @PutMapping("/dashboard/config/tier-aliases")
    @Operation(summary = "Replace dashboard tier alias configuration", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminDashboardTierAliasesResponse> replaceTierAliases(
            @Valid @RequestBody SuperAdminDashboardTierAliasesReplaceRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actorAuthId = principal != null ? principal.getAuthId() : null;
        SuperAdminDashboardTierAliasesResponse body = dashboardOverviewService.replaceTierAliases(request, actorAuthId);
        return ResponseEntity.ok(body);
    }
}

