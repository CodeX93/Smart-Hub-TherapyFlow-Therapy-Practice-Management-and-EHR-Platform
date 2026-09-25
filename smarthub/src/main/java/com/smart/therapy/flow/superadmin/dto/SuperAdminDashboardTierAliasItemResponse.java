package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Dashboard tier alias item")
public class SuperAdminDashboardTierAliasItemResponse {

    @Schema(description = "Dashboard tier name", example = "Pro")
    private String tierName;

    @Schema(description = "Subscription plan code alias", example = "PROFESSIONAL")
    private String planCode;
}
