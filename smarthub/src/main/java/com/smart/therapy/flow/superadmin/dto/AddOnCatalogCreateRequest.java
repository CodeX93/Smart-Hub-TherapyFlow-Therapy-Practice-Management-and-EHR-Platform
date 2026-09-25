package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Create add-on catalog entry")
@Data
public class AddOnCatalogCreateRequest {
    @NotBlank
    @Pattern(regexp = "^[A-Za-z0-9_]+$")
    @Schema(example = "extra_storage")
    private String code;

    @NotBlank
    @Schema(example = "Extra Storage")
    private String name;

    @Schema(example = "Extra storage for documents")
    private String description;

    @NotNull
    @DecimalMin("0.0")
    @Schema(example = "50")
    private BigDecimal priceUsd;

    @NotNull
    @Schema(example = "MONTHLY")
    private AddonBillingCycle billingCycle;

    @Schema(example = "ACTIVE")
    private AddonCatalogStatus status;
}
