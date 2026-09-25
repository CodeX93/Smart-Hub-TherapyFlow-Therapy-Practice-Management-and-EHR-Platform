package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Strict update contract for organisation subscription")
@Data
public class SuperAdminSubscriptionUpdateRequest {
    @Schema(description = "Target plan code/name", example = "pro")
    private String plan;

    @Schema(description = "Billing cycle", allowableValues = {"monthly", "annual"}, example = "annual")
    private String billingCycle;

    @Schema(description = "Trial extension days", allowableValues = {"14", "30"}, example = "14")
    private Integer trialDays;

    @Schema(description = "Effective date for plan change (ISO-8601)", example = "2026-04-01T00:00:00Z")
    private java.time.Instant effectiveDate;

    @Schema(description = "Apply proration when plan changes", example = "true")
    private Boolean prorate;

    @Schema(description = "Create a local renewal invoice when renewing an expired subscription", example = "true")
    private Boolean createRenewalInvoice;

    @Schema(description = "Optional user limits override payload")
    private UserLimitsUpdateRequest userLimits;

    @Data
    public static class UserLimitsUpdateRequest {
        private Integer therapistLimit;
        private Integer supervisorLimit;
        private Integer clientLimit;
    }
}
