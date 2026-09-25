package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminStartImpersonationRequest {
    @NotNull
    private Long targetAuthId;
    @NotNull
    private Long organisationId;
    @NotBlank
    @Size(max = 1000)
    private String reason;
    @Positive
    private Integer durationMinutes;
}
