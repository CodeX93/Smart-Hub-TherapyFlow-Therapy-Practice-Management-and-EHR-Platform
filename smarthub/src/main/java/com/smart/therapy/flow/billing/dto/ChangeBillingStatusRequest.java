package com.smart.therapy.flow.billing.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to change billing status")
public class ChangeBillingStatusRequest {

    @NotNull(message = "Billing status is required")
    @Schema(
            description = "New billing status",
            example = "billed",
            allowableValues = {"pending", "billed", "paid", "denied", "follow_up"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String billingStatus;

    @Schema(description = "Optional notes for status change", example = "Invoice sent to client")
    private String notes;
}
