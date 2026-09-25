package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "Plan pricing tier request")
@Data
public class PlanPricingTierRequest {
    @NotEmpty
    @Valid
    private List<TierItem> tiers;

    @Data
    public static class TierItem {
        @NotNull
        @Min(1)
        @Schema(example = "1")
        private Integer minTherapists;

        @NotNull
        @Min(1)
        @Schema(example = "10")
        private Integer maxTherapists;

        @NotNull
        @DecimalMin("0.0")
        @Schema(example = "40")
        private BigDecimal pricePerTherapistUsd;

        @NotNull
        @Min(0)
        @Schema(example = "5")
        private Integer includedSupervisors;

        @NotNull
        @Min(0)
        @Schema(example = "200")
        private Integer includedClients;
    }
}
