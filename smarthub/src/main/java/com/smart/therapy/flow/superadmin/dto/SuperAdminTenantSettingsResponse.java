package com.smart.therapy.flow.superadmin.dto;

import lombok.Data;

@Data
public class SuperAdminTenantSettingsResponse {
    private String timezone;
    private String region;
    private String dataResidency;
    private String locale;
    private String logoUrl;
    private String brandPrimaryColor;
    private String brandSecondaryColor;
    private String brandAccentColor;
    private String supportEmail;
    private String supportAddress;
}

