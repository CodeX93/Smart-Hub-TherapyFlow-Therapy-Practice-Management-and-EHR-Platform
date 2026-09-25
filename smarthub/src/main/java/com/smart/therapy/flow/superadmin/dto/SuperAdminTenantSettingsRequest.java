package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuperAdminTenantSettingsRequest {
    @NotBlank
    private String timezone;

    @NotBlank
    private String region;

    @NotBlank
    private String dataResidency;

    private String locale;
    private String logoUrl;
    private String brandPrimaryColor;
    private String brandSecondaryColor;
    private String brandAccentColor;
    private String supportEmail;
    private String supportAddress;
}

