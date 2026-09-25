package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Set;

/** Request body for assigning roles to a user (admin-only). */
@Data
@Schema(description = "Request to assign roles to a user. Replaces existing role set for the user.")
public class AssignRoleRequest {

    @NotEmpty(message = "At least one role is required")
    @Schema(description = "Set of role names to assign (replaces existing roles)", example = "[\"THERAPIST\", \"SUPERVISOR\"]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<String> roles;
}
