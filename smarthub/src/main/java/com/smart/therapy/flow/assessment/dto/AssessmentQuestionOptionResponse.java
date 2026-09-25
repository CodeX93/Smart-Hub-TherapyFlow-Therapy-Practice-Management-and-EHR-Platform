package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AssessmentQuestionOptionResponse {

    private Long id;
    private Long questionId;
    private String optionKey;
    private String optionText;
    private String optionValue;
    private BigDecimal scoreValue;
    private Integer sortOrder;
    private Boolean isDefault;
}

