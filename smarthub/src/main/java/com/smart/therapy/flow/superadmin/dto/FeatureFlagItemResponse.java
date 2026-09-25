package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "UI-friendly feature flag row")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureFlagItemResponse {
    private String key;
    private boolean enabled;
    private Integer usageLimit;
}
