package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Schema(description = "Plan update request")
@Data
public class UpdatePlanRequest {
private String name;
private String description;
@DecimalMin("0.0")
private BigDecimal basePrice;
@DecimalMin("0.0")
private BigDecimal annualPrice;
private String billingCycle;
@Min(0)
private Integer trialDays;
private String status;
private String providerPriceIdMonthly;
private String providerPriceIdAnnual;
}
