package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Schema(description = "Plan catalog/pricing response")
@Data
public class PlanPricingResponse {
private String planCode;
private String planName;
private String description;
private BigDecimal basePrice;
private BigDecimal annualPrice;
private String billingCycle;
private Integer trialDays;
private String providerPriceIdMonthly;
private String providerPriceIdAnnual;
private String status;
private Integer therapistLimit;
private Integer supervisorLimit;
private Integer clientLimit;
private List<PlanPricingTierResponse.TierItem> pricingTiers;
private List<PlanEntitlementResponse> entitlements;
}
