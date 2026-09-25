package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to change password (for first login or regular password change)")
public class ChangePasswordRequest {

    @Schema(description = "Change password token (required for first login, optional for regular password change)", 
            example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String changePasswordToken;

    @Schema(description = "Current password (required for regular password change, not needed for first login)", 
            example = "OldPassword123!")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, max = 255, message = "Password must be at least 6 characters")
    @Schema(description = "New password (REQUIRED, minimum 6 characters)", 
            example = "NewSecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String newPassword;
}
