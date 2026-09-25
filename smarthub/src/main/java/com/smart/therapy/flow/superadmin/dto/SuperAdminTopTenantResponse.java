package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Top tenant row for the dashboard")
public class SuperAdminTopTenantResponse {

    @Schema(description = "Organisation id")
    private Long organisationId;

    @Schema(description = "Organisation name")
    private String organisationName;

    @Schema(description = "Subscription plan name", example = "Pro")
    private String planName;

    @Schema(description = "Subscription plan code", example = "PRO")
    private String planCode;

    @Schema(description = "Active staff users in the organisation (is_active = true)")
    private long activeUsers;
}
