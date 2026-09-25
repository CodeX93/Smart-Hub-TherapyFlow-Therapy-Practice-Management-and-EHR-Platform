package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Form field response")
public class FormFieldResponse {
    @Schema(description = "Assignment field ID (use as assignmentFieldId when saving responses)", example = "1")
    private Long id;

    @Schema(description = "Same as id; included for clients that expect an explicit assignmentFieldId", example = "1")
    private Long assignmentFieldId;
    
    @Schema(description = "Template version ID this field belongs to", example = "1")
    private Long templateVersionId;
    
    @Schema(description = "Section ID this field belongs to", example = "1")
    private Long sectionId;
    
    @Schema(description = "Section name", example = "Personal Information")
    private String sectionName;
    
    @Schema(description = "Field type", example = "text", allowableValues = {"text", "textarea", "select", "checkbox", "checkbox_group", "radio", "date", "signature", "file", "heading", "info_text"})
    private String fieldType;
    
    @Schema(description = "Field label", example = "Full Name")
    private String label;
    
    @Schema(description = "Placeholder text", example = "Enter your full name")
    private String placeholder;
    
    @Schema(description = "Help text for the field", example = "Enter your legal name")
    private String helpText;
    
    @Schema(description = "Whether the field is required", example = "true")
    private Boolean isRequired;
    
    @Schema(description = "Field options (normalized)", example = "[]")
    private List<FormFieldOptionResponse> options;
    
    @Schema(description = "Validation rules (JSONB)", example = "{\"minLength\": 2, \"maxLength\": 100}")
    private String validation;
    
    @Schema(description = "Default value", example = "John Doe")
    private String defaultValue;
    
    @Schema(description = "Whether field is repeatable", example = "false")
    private Boolean isRepeatable;
    
    @Schema(description = "Scoring formula (for assessments)", example = "score * 2")
    private String scoringFormula;
    
    @Schema(description = "Maximum score", example = "100")
    private Integer maxScore;
    
    @Schema(description = "Auto-populate source", example = "client_name", 
            allowableValues = {"client_name", "client_dob", "client_address", "therapist_name", "practice_name"})
    private String autoPopulate;
    
    @Schema(description = "Conditional display rules (JSON)", example = "{\"showIf\": {\"fieldId\": 1, \"value\": \"yes\"}}")
    private String conditionalDisplay;
    
    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;
    
    @Schema(description = "Timestamp when field was created", example = "2025-12-31T12:00:00Z")
    private Instant createdAt;
    
    @Schema(description = "Timestamp when field was last updated", example = "2025-12-31T12:00:00Z")
    private Instant updatedAt;
}

