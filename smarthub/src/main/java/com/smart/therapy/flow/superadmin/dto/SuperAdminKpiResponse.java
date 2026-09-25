package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Schema(description = "Global KPI aggregates for super admin dashboard")
public class SuperAdminKpiResponse {

    @Schema(description = "Requested period window", example = "30d")
    private String period;

    @Schema(description = "Optional organisation id scope")
    private Long orgId;

    @Schema(description = "Organisation name when scoped")
    private String orgName;

    @Schema(description = "Currency code for revenue figures", example = "USD")
    private String currency;

    @Schema(description = "Timezone used for day boundaries", example = "UTC")
    private String timezone;

    @Schema(description = "Inclusive period start timestamp")
    private Instant from;

    @Schema(description = "Exclusive period end timestamp")
    private Instant to;

    @Schema(description = "Active staff users in period")
    private long activeUsers;

    @Schema(description = "Active client users in period")
    private long activeClients;

    @Schema(description = "Completed sessions in period")
    private long sessionsCompleted;

    @Schema(description = "Revenue in USD for period (pre-tax, pre-credit)")
    private BigDecimal monthlyRevenueUsd;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
