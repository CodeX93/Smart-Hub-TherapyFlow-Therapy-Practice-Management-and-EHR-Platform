package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Schema(description = "Dashboard tier aliases response")
public class SuperAdminDashboardTierAliasesResponse {

    @Schema(description = "Tier alias rows")
    private List<SuperAdminDashboardTierAliasItemResponse> aliases;

    @Schema(description = "Response generation timestamp")
    private Instant generatedAt;
}
