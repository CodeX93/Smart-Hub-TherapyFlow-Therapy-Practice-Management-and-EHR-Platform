package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Single plan entitlement row")
@Data
public class PlanEntitlementItem {
private String key;
private String featureCode;
private Boolean enabled;
private Integer usageLimit;
private Boolean trialAvailable;

public String resolveFeatureCode() {
    if (key != null && !key.isBlank()) {
        return key;
    }
    return featureCode;
}
}
