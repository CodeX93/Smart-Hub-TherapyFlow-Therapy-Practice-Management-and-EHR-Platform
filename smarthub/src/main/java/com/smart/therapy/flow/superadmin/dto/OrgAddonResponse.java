package com.smart.therapy.flow.superadmin.dto;

import com.smart.therapy.flow.subscription.enums.AddonBillingCycle;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.Instant;
import lombok.Data;

@Schema(description = "Organisation add-on purchase row")
@Data
public class OrgAddonResponse {
private Long purchaseId;
private String featureCode;
private String featureName;
private Integer quantity;
private BigDecimal pricePerUnitAtTime;
private Integer unitValue;
private AddonBillingCycle billingCycle;
private Instant startAt;
private Instant endAt;
}
