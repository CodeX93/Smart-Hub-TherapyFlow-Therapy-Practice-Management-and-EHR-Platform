package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to activate client portal account")
public class PortalActivateRequest {
    @NotBlank(message = "Activation token is required")
    @Schema(description = "Activation token from email (REQUIRED)", example = "abc123def456", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Password to set for portal access (REQUIRED, minimum 6 characters)", example = "ClientPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;
}

