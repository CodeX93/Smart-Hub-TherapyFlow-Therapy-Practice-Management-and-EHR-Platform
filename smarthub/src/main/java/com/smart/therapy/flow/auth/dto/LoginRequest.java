package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.annotation.JsonAlias;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Login request for staff/admin/therapist authentication")
public class LoginRequest {

    @NotBlank(message = "Username or email is required")
    @JsonAlias({"email", "loginIdentifier"})
    @Schema(description = "Username or email for authentication", example = "admin@therapyflow.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @NotBlank(message = "Password is required")
    @Schema(description = "Password for authentication", example = "SecurePassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Organisation identifier type for multi-org routing (slug or id)", example = "slug")
    private String orgIdentifier;

    @Schema(description = "Organisation identifier value (slug or id)", example = "acme-clinic")
    private String orgValue;

    @JsonAlias({"orgId", "organisationId"})
    private String orgId;

    @JsonAlias({"orgSlug"})
    private String orgSlug;

    @Schema(description = "Opaque device trust token from a prior 'trust this device' MFA login")
    private String deviceTrustToken;

    @Schema(description = "When true after MFA, mark this browser/device as trusted for ~30 days")
    private Boolean trustDevice;

    @Schema(description = "When true, issue a longer-lived refresh token (stay signed in)")
    private Boolean staySignedIn;

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}

