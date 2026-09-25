package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request body for staff password reset using the token from the forgot-password email.
 */
@Data
@Schema(description = "Request to reset staff password using the token received by email")
public class StaffResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    @Schema(description = "Password reset token from the forgot-password email", example = "abc123xyz789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    @Schema(description = "New password (min 6 characters)", example = "NewSecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED, minLength = 6)
    private String newPassword;
}
