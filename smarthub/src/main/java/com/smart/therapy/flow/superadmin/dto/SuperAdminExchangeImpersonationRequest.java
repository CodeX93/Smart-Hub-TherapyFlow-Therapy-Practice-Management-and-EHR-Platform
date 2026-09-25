package com.smart.therapy.flow.superadmin.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SuperAdminExchangeImpersonationRequest {
    @NotBlank
    private String impersonationToken;
}
