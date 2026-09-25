package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Upsert add-on catalog price request")
@Data
public class UpsertAddonCatalogRequest {
    @NotNull
    @DecimalMin(value = "0.0", inclusive = true)
    private BigDecimal pricePerUnit;

    @NotNull
    private AddonBillingCycle billingCycle;

    @NotNull
    @Min(1)
    private Integer unitValue;

    private AddonCatalogStatus status;
}
