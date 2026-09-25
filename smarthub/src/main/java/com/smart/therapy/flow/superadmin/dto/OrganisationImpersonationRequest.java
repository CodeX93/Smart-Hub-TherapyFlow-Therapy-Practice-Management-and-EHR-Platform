package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrganisationImpersonationRequest {

    @NotNull
    private Long targetAuthId;

    private String reason;

    @Min(1)
    private Integer durationMinutes;
}

