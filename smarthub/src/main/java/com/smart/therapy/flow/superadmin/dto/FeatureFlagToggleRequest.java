package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FeatureFlagToggleRequest {
    @NotNull
    private Boolean enabled;
}
