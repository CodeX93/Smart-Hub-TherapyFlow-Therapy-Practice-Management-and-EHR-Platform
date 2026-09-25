package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Form template version response")
public class FormTemplateVersionResponse {
    @Schema(description = "Version ID", example = "1")
    private Long id;
    
    @Schema(description = "Template ID", example = "1")
    private Long templateId;
    
    @Schema(description = "Version number", example = "1")
    private Long versionNumber;
    
    @Schema(description = "Version name (optional override)", example = "Intake Form v1.0")
    private String name;
    
    @Schema(description = "Version description (optional override)")
    private String description;
    
    @Schema(description = "Version instructions (optional override)")
    private String instructions;
    
    @Schema(description = "Version status", example = "ACTIVE", allowableValues = {"DRAFT", "ACTIVE", "ARCHIVED"})
    private String status;
    
    @Schema(description = "ID of the user who created this version", example = "1")
    private Long createdById;
    
    @Schema(description = "Name of the user who created this version", example = "John Doe")
    private String createdByName;
    
    @Schema(description = "Timestamp when version was created")
    private Instant createdAt;
    
    @Schema(description = "List of sections in this version")
    private List<FormSectionResponse> sections;
    
    @Schema(description = "List of fields in this version (flattened, for backward compatibility)")
    private List<FormFieldResponse> fields;
}
