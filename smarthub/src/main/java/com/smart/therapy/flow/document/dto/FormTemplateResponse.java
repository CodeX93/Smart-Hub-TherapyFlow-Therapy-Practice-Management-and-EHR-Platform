package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Form template response")
public class FormTemplateResponse {
    @Schema(description = "Template ID", example = "1")
    private Long id;
    
    @Schema(description = "Template name", example = "Intake Form")
    private String name;
    
    @Schema(description = "Template description", example = "Initial intake form for new clients")
    private String description;
    
    @Schema(description = "Template category", example = "intake", allowableValues = {"consent", "intake", "release", "agreement", "safety", "discharge", "custom"})
    private String category;
    
    @Schema(description = "Instructions for filling out the form", example = "Please fill out all required fields")
    private String instructions;
    
    @Schema(description = "Whether the form requires a signature", example = "true")
    private Boolean requiresSignature;
    
    @Schema(description = "Whether the template is active", example = "true")
    private Boolean isActive;
    
    @Schema(description = "Whether the template is deleted (soft delete)", example = "false")
    private Boolean isDeleted;
    
    @Schema(description = "Whether this is a system template", example = "false")
    private Boolean isSystemTemplate;
    
    @Schema(description = "Sort order for display", example = "1")
    private Integer sortOrder;
    
    @Schema(description = "ID of the user who created the template", example = "1")
    private Long createdById;
    
    @Schema(description = "Name of the user who created the template", example = "John Doe")
    private String createdByName;
    
    @Schema(description = "Timestamp when template was deleted (null if not deleted)", example = "2025-12-31T12:00:00Z")
    private Instant deletedAt;
    
    @Schema(description = "Timestamp when template was created", example = "2025-12-31T12:00:00Z")
    private Instant createdAt;
    
    @Schema(description = "Timestamp when template was last updated", example = "2025-12-31T12:00:00Z")
    private Instant updatedAt;
    
    @Schema(description = "List of form fields in this template (deprecated - use versions)")
    private List<FormFieldResponse> fields;
    
    @Schema(description = "List of versions for this template")
    private List<FormTemplateVersionResponse> versions;
    
    @Schema(description = "Active version (if any)")
    private FormTemplateVersionResponse activeVersion;
}

