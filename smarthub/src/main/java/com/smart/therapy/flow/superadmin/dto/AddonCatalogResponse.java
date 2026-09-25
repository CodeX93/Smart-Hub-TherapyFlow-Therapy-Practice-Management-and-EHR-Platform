package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import com.smart.therapy.flow.subscription.enums.AddonCatalogStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import lombok.Data;

@Schema(description = "Add-on catalog row")
@Data
public class AddonCatalogResponse {
    private String featureCode;
    private String featureName;
    private String description;
    private BigDecimal pricePerUnit;
    private AddonBillingCycle billingCycle;
    private Integer unitValue;
    private AddonCatalogStatus status;
}
