package com.smart.therapy.flow.client.portal.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to initiate password reset")
public class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Client's portal email address (REQUIRED)", example = "client@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    private String email;

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
}

