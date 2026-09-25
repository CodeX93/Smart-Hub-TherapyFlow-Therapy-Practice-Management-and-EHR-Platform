package com.smart.therapy.flow.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantResolveRequest {
    @NotBlank
    @Email
    private String email;
}

