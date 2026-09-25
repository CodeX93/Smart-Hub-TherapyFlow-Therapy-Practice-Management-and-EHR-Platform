package com.smart.therapy.flow.client.portal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Client portal login request")
public class PortalLoginRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Client's portal email address", example = "client@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;
    
    @NotBlank(message = "Password is required")
    @Schema(description = "Client's portal password", example = "ClientPassword123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

    @Schema(description = "Organisation identifier type for multi-org routing (slug or id)", example = "slug")
    private String orgIdentifier;

    @Schema(description = "Organisation identifier value (slug or id)", example = "acme-clinic")
    private String orgValue;

    @JsonAlias({"orgId", "organisationId"})
    @Schema(description = "Organisation id for multi-org routing", example = "42")
    private String orgId;

    @JsonAlias({"orgSlug"})
    @Schema(description = "Organisation slug for multi-org routing", example = "acme-clinic")
    private String orgSlug;

    @Schema(description = "Opaque device trust token from a prior 'trust this device' MFA login")
    private String deviceTrustToken;

    @Schema(description = "When true after MFA, mark this browser/device as trusted for ~30 days")
    private Boolean trustDevice;

    @Schema(description = "When true, issue a longer-lived refresh token (stay signed in)")
    private Boolean staySignedIn;
}

