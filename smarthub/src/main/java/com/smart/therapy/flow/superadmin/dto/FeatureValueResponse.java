package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Effective feature state")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FeatureValueResponse {
    private boolean enabled;
    private Integer usageLimit;
}
