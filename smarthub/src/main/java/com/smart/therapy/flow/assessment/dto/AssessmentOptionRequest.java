package com.smart.therapy.flow.assessment.dto;

import lombok.Data;

@Data
public class AssessmentOptionRequest {

    private String optionText;
    private Double optionValue;
    private Integer sortOrder = 0;
}

