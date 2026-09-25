package com.smart.therapy.flow.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class QuestionResponseRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String responseText;
    private Double scoreValue; // Legacy: pre-calculated score (will be recalculated)
    private Integer selectedOptionId; // Legacy: single option ID
    private List<Integer> selectedOptionIds; // Array of option IDs (preferred)
    private Integer ratingValue; // For rating scale questions
}

