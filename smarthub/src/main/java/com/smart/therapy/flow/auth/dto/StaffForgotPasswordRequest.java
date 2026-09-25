package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for staff (admin/therapist/supervisor) forgot-password.
 * Sends a password reset link to the user's email if the account exists.
 */
@Data
@Schema(description = "Request to initiate staff password reset. Sends reset email if the account exists.")
public class StaffForgotPasswordRequest {

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Schema(description = "Staff email address (login identifier)", example = "therapist@therapyflow.com", requiredMode = Schema.RequiredMode.REQUIRED)
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
