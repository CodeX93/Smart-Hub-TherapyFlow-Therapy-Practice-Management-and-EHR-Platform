package com.smart.therapy.flow.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreatePermissionRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    @NotBlank(message = "Display name is required")
    private String displayName;
    
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category; // client_management, scheduling, etc.
    
    private Boolean isActive;
}

