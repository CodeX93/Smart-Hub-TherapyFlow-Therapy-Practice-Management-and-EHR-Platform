package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Revenue totals for a plan in the selected window")
public class SuperAdminRevenueByPlanResponse {

    @Schema(description = "Plan name", example = "Enterprise")
    private String planName;

    @Schema(description = "Revenue collected for the plan in the window")
    private BigDecimal amount;
}
