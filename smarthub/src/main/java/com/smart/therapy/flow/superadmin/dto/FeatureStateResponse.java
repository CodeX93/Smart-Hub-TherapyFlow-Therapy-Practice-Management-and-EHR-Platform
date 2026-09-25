package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Effective feature state and optional limit")
@Data
public class FeatureStateResponse {
private String featureKey;
private Boolean enabled;
private Integer usageLimit;
}
