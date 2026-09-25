package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuperAdminResolveTenantRequest {
    @NotBlank
    @Email
    private String email;
}
