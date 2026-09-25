package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to create a new form field")
public class CreateFormFieldRequest {
    @Schema(description = "Template ID (optional when creating fields as part of template creation)", example = "1")
    private Long templateId; // Optional: not required when creating fields as part of template creation
    
    @NotBlank(message = "Field type is required")
    private String fieldType; // text, textarea, select, checkbox, checkbox_group, radio, date, signature, file, heading, info_text
    
    @NotBlank(message = "Label is required")
    private String label;
    
    private String placeholder;
    
    private String helpText;
    
    private Boolean isRequired;
    
    private String options; // JSON array or comma-separated
    
    private String validation; // JSON
    
    private String defaultValue;
    
    private String autoPopulate; // client_name, client_dob, etc.
    
    private String conditionalDisplay; // JSON
    
    private Integer sortOrder;
}

