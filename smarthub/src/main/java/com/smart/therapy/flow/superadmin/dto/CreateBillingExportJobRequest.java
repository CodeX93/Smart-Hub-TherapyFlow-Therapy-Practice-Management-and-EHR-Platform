package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.BillingExportType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Schema(description = "Create billing export job request")
@Data
public class CreateBillingExportJobRequest {
    @NotNull
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED, example = "BILLING_INVOICES_CSV")
    private BillingExportType exportType;

    @Positive
    private Long organisationId;

    private String status;

    @Min(1)
    @Max(24)
    private Integer months;
}
