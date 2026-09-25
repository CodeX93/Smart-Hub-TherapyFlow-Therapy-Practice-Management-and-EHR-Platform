package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SuperAdminDisableUserRequest {
    @NotBlank
    @Size(max = 1000)
    private String reason;
}
