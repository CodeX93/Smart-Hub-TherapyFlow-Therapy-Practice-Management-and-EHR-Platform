package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@Schema(description = "Password reset token validation result")
public class PasswordResetTokenValidationResponse {

    @Schema(description = "Whether the token exists and has not expired")
    boolean valid;

    @Schema(description = "Account type when valid: staff or client")
    String accountType;

    @Schema(description = "Human-readable message when invalid")
    String message;
}
