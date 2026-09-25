package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminTerminateRequest {
    @Size(min = 5, max = 500)
    @NotBlank
    private String reason;

    private Integer retentionDays;
}
