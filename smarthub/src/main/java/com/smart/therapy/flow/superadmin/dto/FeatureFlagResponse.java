package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Feature flag for an organisation")
@Data
public class FeatureFlagResponse {
private String featureKey;
private boolean enabled;
}
