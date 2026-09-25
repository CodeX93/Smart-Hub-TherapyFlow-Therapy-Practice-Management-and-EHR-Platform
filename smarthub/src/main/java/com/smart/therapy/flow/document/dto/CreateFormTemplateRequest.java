package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request to create a new form template")
public class CreateFormTemplateRequest {
    @NotBlank(message = "Name is required")
    @Schema(description = "Form template name (REQUIRED)", example = "Intake Form", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;
    
    @Schema(description = "Form template description (optional)", example = "Initial intake form for new clients")
    private String description;
    
    @NotBlank(message = "Category is required")
    @Schema(description = "Form category (REQUIRED)", example = "intake", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"consent", "intake", "release", "agreement", "safety", "discharge", "custom"})
    private String category; // consent, intake, release, agreement, safety, discharge, custom
    
    @Schema(description = "Instructions for filling out the form (optional)", example = "Please fill out all required fields")
    private String instructions;
    
    @NotNull(message = "Requires signature must be specified")
    @Schema(description = "Whether the form requires a signature (REQUIRED)", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean requiresSignature;
    
    @Schema(description = "Whether the template is active (optional, default: true)", example = "true", defaultValue = "true")
    private Boolean isActive;
    
    @Schema(description = "Whether this is a system template (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean isSystemTemplate;
    
    @Schema(description = "Sort order for display (optional)", example = "1")
    private Integer sortOrder;
    
    @Schema(description = "List of form fields (optional). Note: templateId is not required in field objects when creating as part of template.", example = "[]")
    private List<CreateFormFieldInTemplateRequest> fields;
}

