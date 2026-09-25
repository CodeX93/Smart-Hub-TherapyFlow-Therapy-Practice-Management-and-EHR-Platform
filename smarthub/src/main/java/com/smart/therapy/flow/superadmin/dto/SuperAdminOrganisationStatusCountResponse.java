package com.smart.therapy.flow.superadmin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Organisation status count bucket")
public class SuperAdminOrganisationStatusCountResponse {

    @Schema(description = "Status bucket", example = "Active")
    private String status;

    @Schema(description = "Total organisations in this bucket")
    private long count;
}
