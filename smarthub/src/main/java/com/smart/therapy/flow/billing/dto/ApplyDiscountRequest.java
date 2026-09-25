package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "Request to apply a discount to a session billing")
public class ApplyDiscountRequest {

    @Schema(
            description = "Type of discount to apply",
            example = "percentage",
            allowableValues = {"percentage", "fixed", "none"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String discountType;

    @Schema(
            description = "Discount value. For 'percentage': value between 0-100 (e.g., 15.00 for 15% off). For 'fixed': dollar amount (e.g., 50.00 for $50 off). Not used for 'none'.",
            example = "15.00"
    )
    private BigDecimal discountValue;

    @Schema(
            description = "Explicit discount amount in dollars. Optional for 'fixed' type. Calculated automatically for 'percentage' type.",
            example = "50.00"
    )
    private BigDecimal discountAmount;
}

