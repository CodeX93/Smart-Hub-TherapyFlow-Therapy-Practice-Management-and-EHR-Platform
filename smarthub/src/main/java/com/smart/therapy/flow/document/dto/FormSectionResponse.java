package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Form section response")
public class FormSectionResponse {
    @Schema(description = "Section ID", example = "1")
    private Long id;
    
    @Schema(description = "Template version ID", example = "1")
    private Long templateVersionId;
    
    @Schema(description = "Section name", example = "Personal Information")
    private String name;
    
    @Schema(description = "Section description")
    private String description;
    
    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;
    
    @Schema(description = "List of fields in this section")
    private List<FormFieldResponse> fields;
    
    @Schema(description = "Timestamp when section was created")
    private Instant createdAt;
}
