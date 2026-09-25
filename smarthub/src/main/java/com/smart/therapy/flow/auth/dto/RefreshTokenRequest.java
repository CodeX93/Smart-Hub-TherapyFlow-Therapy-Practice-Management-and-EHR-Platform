package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to refresh access token")
public class RefreshTokenRequest {

    @NotBlank(message = "Refresh token is required")
    @Schema(description = "Refresh token obtained from login (REQUIRED)", example = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...", requiredMode = Schema.RequiredMode.REQUIRED)
    private String refreshToken;

    public String getRefreshToken() {
        return refreshToken;
    }
}

