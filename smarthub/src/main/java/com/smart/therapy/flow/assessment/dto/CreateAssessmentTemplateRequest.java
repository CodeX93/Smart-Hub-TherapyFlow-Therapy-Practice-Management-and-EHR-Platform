package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "Request to create a new assessment template")
public class CreateAssessmentTemplateRequest {

    @NotBlank(message = "Name is required")
    @Schema(description = "Assessment template name (REQUIRED)", example = "PHQ-9 Depression Assessment", requiredMode = Schema.RequiredMode.REQUIRED)
    private String name;

    @Schema(description = "Assessment template description (optional)", example = "Patient Health Questionnaire for depression screening")
    private String description;

    @Schema(description = "Assessment category (optional)", example = "Mental Health")
    private String category;

    @Schema(description = "Whether this is a standardized assessment (optional, default: false)", example = "true", defaultValue = "false")
    private Boolean isStandardized = false;

    @Schema(description = "Template version (optional, default: 1)", example = "1", defaultValue = "1")
    private Integer version = 1;

    @Schema(description = "List of assessment sections/questions (optional)", example = "[]")
    private List<AssessmentSectionRequest> sections;
}

