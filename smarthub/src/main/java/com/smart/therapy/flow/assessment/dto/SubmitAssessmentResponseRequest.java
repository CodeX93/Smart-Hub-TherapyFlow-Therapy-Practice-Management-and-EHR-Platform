package com.smart.therapy.flow.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitAssessmentResponseRequest {

    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;

    private List<QuestionResponseRequest> responses;
}

