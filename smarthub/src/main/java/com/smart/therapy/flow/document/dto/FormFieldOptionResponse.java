package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@Schema(description = "Form field option response")
public class FormFieldOptionResponse {
    @Schema(description = "Option ID", example = "1")
    private Long id;
    
    @Schema(description = "Field ID", example = "1")
    private Long fieldId;
    
    @Schema(description = "Display label", example = "Yes")
    private String label;
    
    @Schema(description = "Option value", example = "yes")
    private String value;
    
    @Schema(description = "Score (for assessments)", example = "10")
    private Integer score;
    
    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;
}
