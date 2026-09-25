package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Schema(description = "KPI card summary for super admin dashboard")
public class SuperAdminDashboardKpiCardsResponse {

    @Schema(description = "Timezone used for date calculations", example = "UTC")
    private String timezone;

    @Schema(description = "Display currency", example = "USD")
    private String currency;

    @Schema(description = "Month window start used for churn calculations")
    private Instant monthStart;

    @Schema(description = "Month window end used for churn calculations")
    private Instant monthEnd;

    @Schema(description = "Active tenant count")
    private long activeTenants;

    @Schema(description = "Total active tenant end users (staff + portal clients); excludes platform identities")
    private long totalEndUsers;

    @Schema(description = "Active tenant staff login accounts")
    private long staffUsers;

    @Schema(description = "Active client portal login accounts")
    private long portalClients;

    @Schema(description = "Monthly recurring revenue")
    private BigDecimal mrr;

    @Schema(description = "Tenant churn count in current month")
    private long churnThisMonth;

    @Schema(description = "Platform availability over the trailing window, from recorded outage incidents")
    private BigDecimal platformUptimePercent;

    @Schema(description = "Length in days of the window the availability figure covers", example = "30")
    private long uptimeWindowDays;

    @Schema(description = "Recorded outage minutes inside the availability window")
    private long downtimeMinutes;

    @Schema(description = "Platform incidents recorded in the window, including non-outage severities")
    private long incidentsInWindow;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
