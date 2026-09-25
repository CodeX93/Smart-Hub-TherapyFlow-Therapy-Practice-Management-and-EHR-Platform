package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Plan create request")
@Data
public class CreatePlanRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]+$")
    private String code;

    @NotBlank
    private String name;

    @NotBlank
    private String billingCycle;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal basePrice;

    @DecimalMin("0.0")
    private BigDecimal annualPrice;

    private String description;

    @Min(0)
    private Integer trialDays;

    private String status;

    private String providerPriceIdMonthly;

    private String providerPriceIdAnnual;
}
