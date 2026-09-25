package com.smart.therapy.flow.assessment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
@Schema(description = "Request to update an assessment template")
public class UpdateAssessmentTemplateRequest extends PatchAwareRequest {

    @Schema(description = "Assessment template name", example = "PHQ-9 Depression Assessment")
    private String name;

    @Schema(description = "Assessment template description", example = "Patient Health Questionnaire for depression screening")
    private String description;

    @Schema(description = "Assessment category", example = "Mental Health")
    private String category;

    @Schema(description = "Whether this is a standardized assessment", example = "true")
    private Boolean isStandardized;

    @Schema(description = "Template version", example = "1.1")
    private String version;

    @Schema(description = "Whether the template is active", example = "true")
    private Boolean isActive;

    @Schema(description = "List of assessment sections/questions")
    private List<AssessmentSectionRequest> sections;
}
