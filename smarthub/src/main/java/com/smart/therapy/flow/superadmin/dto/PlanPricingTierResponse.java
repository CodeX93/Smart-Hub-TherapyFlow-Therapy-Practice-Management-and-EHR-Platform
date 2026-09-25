package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "Plan pricing tier response")
@Data
public class PlanPricingTierResponse {
    @Schema(example = "plan_enterprise")
    private String planId;
    private List<TierItem> tiers;

    @Data
    public static class TierItem {
        @Schema(example = "1")
        private Integer minTherapists;
        @Schema(example = "10")
        private Integer maxTherapists;
        @Schema(example = "40")
        private BigDecimal pricePerTherapistUsd;
        @Schema(example = "5")
        private Integer includedSupervisors;
        @Schema(example = "200")
        private Integer includedClients;
    }
}
