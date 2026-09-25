package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to reset password using reset token")
public class ResetPasswordRequest {
    @NotBlank(message = "Reset token is required")
    @Schema(description = "Password reset token from email (REQUIRED)", example = "xyz789abc123", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "New password (REQUIRED, minimum 6 characters)", example = "NewPassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String password;
}

