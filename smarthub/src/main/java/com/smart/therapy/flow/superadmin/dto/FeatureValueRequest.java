package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Feature value payload")
@Data
public class FeatureValueRequest {
    private Boolean enabled;
    private Integer usageLimit;
}
