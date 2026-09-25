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
@Schema(description = "Request to update an assessment section")
public class UpdateAssessmentSectionRequest extends PatchAwareRequest {

    @Schema(description = "Section title", example = "Depression Symptoms")
    private String title;

    @Schema(description = "Section description", example = "Questions about depression symptoms")
    private String description;

    @Schema(description = "Access level", example = "client")
    private String accessLevel;

    @Schema(description = "Whether this section contributes to scoring", example = "true")
    private Boolean isScoring;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "AI report mapping key for this section", example = "symptoms_overview")
    private String reportMapping;

    @Schema(description = "AI prompt used when generating reports from this section")
    private String aiReportPrompt;

    @Schema(description = "List of questions in this section")
    private List<AssessmentQuestionRequest> questions;
}
