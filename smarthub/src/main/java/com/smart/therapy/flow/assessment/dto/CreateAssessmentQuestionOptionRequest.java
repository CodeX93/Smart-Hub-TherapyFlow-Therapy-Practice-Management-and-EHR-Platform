package com.smart.therapy.flow.assessment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class CreateAssessmentQuestionOptionRequest {

    @NotNull(message = "Question ID is required")
    private Long questionId;

    private String optionKey;
    @NotNull(message = "Option text is required")
    private String optionText;
    private String optionValue;
    private BigDecimal scoreValue;
    private Integer sortOrder;
    private Boolean isDefault;
}

