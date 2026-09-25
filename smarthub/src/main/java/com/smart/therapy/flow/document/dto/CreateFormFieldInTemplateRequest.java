package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to create a form field as part of template creation (templateId not required)")
public class CreateFormFieldInTemplateRequest {
    @Schema(description = "Optional template id. Ignored for template-level create/update payloads.", example = "5")
    private Long templateId;

    @NotBlank(message = "Field type is required")
    @Schema(description = "Field type (REQUIRED)", example = "text", requiredMode = Schema.RequiredMode.REQUIRED, 
            allowableValues = {"text", "textarea", "select", "checkbox", "checkbox_group", "radio", "date", "signature", "file", "heading", "info_text"})
    private String fieldType;
    
    @NotBlank(message = "Label is required")
    @Schema(description = "Field label (REQUIRED)", example = "Full Name", requiredMode = Schema.RequiredMode.REQUIRED)
    private String label;
    
    @Schema(description = "Placeholder text for input fields", example = "Enter your full name")
    private String placeholder;
    
    @Schema(description = "Help text displayed to users", example = "Enter your legal name as it appears on official documents")
    private String helpText;
    
    @Schema(description = "Whether the field is required", example = "true", defaultValue = "false")
    private Boolean isRequired;
    
    @Schema(description = "Options for select/radio/checkbox_group fields (JSON array or comma-separated)", example = "[\"Option 1\", \"Option 2\"]")
    private String options;
    
    @Schema(description = "Validation rules (JSON)", example = "{\"minLength\": 2, \"maxLength\": 100}")
    private String validation;
    
    @Schema(description = "Default value for the field", example = "John Doe")
    private String defaultValue;
    
    @Schema(description = "Auto-populate source", example = "client_name", 
            allowableValues = {"client_name", "client_dob", "client_address", "therapist_name", "practice_name"})
    private String autoPopulate;
    
    @Schema(description = "Conditional display rules (JSON)", example = "{\"showIf\": {\"fieldId\": 1, \"value\": \"yes\"}}")
    private String conditionalDisplay;
    
    @Schema(description = "Sort order for field display", example = "1", defaultValue = "0")
    private Integer sortOrder;
}

