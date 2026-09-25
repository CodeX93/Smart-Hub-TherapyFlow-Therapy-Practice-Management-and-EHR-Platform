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
@Schema(description = "Request to update an assessment question")
public class UpdateAssessmentQuestionRequest extends PatchAwareRequest {

    @Schema(description = "Question text", example = "Over the past 2 weeks, how often have you felt down?")
    private String questionText;

    @Schema(description = "Question type (text, multiple_choice, rating, scale, yes_no)", example = "multiple_choice")
    private String questionType;

    @Schema(description = "Whether this question is required", example = "true")
    private Boolean isRequired;

    @Schema(description = "Sort order", example = "1")
    private Integer sortOrder;

    @Schema(description = "Minimum rating value", example = "0")
    private Double ratingMin;

    @Schema(description = "Maximum rating value", example = "10")
    private Double ratingMax;

    @Schema(description = "Rating labels (comma-separated)", example = "Not at all,Several days,More than half,Nearly every day")
    private String ratingLabels;

    @Schema(description = "Whether this question contributes to score", example = "true")
    private Boolean contributesToScore;

    @Schema(description = "List of options for this question")
    private List<AssessmentOptionRequest> options;
}
