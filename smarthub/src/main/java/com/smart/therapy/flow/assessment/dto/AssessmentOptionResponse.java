package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssessmentOptionResponse {

    private Long id;
    private String optionText;
    private Double optionValue;
    private Integer sortOrder;
}

