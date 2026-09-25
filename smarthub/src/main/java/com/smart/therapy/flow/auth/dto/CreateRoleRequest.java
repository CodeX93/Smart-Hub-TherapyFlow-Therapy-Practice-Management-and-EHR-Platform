package com.smart.therapy.flow.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request to create a new role")
public class CreateRoleRequest {
    @NotBlank(message = "Name is required")
    @Size(max = 50, message = "Role name must be at most 50 characters")
    @Schema(description = "Role name/identifier (REQUIRED, must be unique)", example = "THERAPIST", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @NotBlank(message = "Display name is required")
    @Size(max = 100, message = "Display name must be at most 100 characters")
    @Schema(description = "Human-readable display name (REQUIRED)", example = "Therapist", requiredMode = Schema.RequiredMode.REQUIRED)
    private String displayName;
    
    @Schema(description = "Role description (optional)", example = "Therapist role with access to client management and sessions")
    private String description;
    
    @Schema(description = "Whether this is a system role (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean isSystem;
    
    @Schema(description = "Whether the role is active (optional, default: true)", example = "true", defaultValue = "true")
    private Boolean isActive;
    
    @Schema(description = "List of permission IDs to assign to this role (optional)", example = "[1, 2, 3]")
    private List<Long> permissions; // List of permission IDs
}

