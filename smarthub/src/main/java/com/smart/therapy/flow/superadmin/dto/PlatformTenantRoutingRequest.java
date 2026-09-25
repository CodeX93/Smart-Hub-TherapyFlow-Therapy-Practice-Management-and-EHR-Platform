package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PlatformTenantRoutingRequest {
    @NotNull
    private Boolean emailAutoRouting;

    @NotNull
    private Boolean pathBasedRouting;

    @NotBlank
    private String pathPrefix;

    @NotBlank
    private String orgIdentifier;
}

