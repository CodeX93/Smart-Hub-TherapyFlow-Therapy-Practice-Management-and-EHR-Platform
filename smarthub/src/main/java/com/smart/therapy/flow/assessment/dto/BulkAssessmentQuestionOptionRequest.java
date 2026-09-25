package com.smart.therapy.flow.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BulkAssessmentQuestionOptionRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    @NotNull(message = "Options are required")
    private List<CreateAssessmentQuestionOptionRequest> options;
}

