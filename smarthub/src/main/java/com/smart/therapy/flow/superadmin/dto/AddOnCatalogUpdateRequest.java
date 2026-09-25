package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Update add-on catalog entry")
@Data
public class AddOnCatalogUpdateRequest {
    @Schema(example = "Extra Storage")
    private String name;

    @Schema(example = "Extra storage for documents")
    private String description;

    @DecimalMin("0.0")
    @Schema(example = "60")
    private BigDecimal priceUsd;

    @Schema(example = "ANNUAL")
    private AddonBillingCycle billingCycle;

    @Schema(example = "ACTIVE")
    private AddonCatalogStatus status;
}
