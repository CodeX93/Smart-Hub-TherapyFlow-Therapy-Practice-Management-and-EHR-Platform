package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Add-on catalog item response")
@Data
public class AddOnCatalogItemResponse {
    @Schema(example = "addon_extra_storage")
    private String id;
    @Schema(example = "extra_storage")
    private String code;
    @Schema(example = "Extra Storage")
    private String name;
    @Schema(example = "Extra storage for documents")
    private String description;
    @Schema(example = "50")
    private BigDecimal priceUsd;
    @Schema(example = "MONTHLY")
    private AddonBillingCycle billingCycle;
    @Schema(example = "ACTIVE")
    private AddonCatalogStatus status;
}
