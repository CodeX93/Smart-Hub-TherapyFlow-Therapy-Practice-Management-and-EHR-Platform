package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssessmentQuestionResponse {

    private Long id;
    private String questionText;
    private String questionType;
    private Boolean isRequired;
    private Integer sortOrder;
    private Double ratingMin;
    private Double ratingMax;
    private String ratingLabels;
    private Boolean contributesToScore;
    private List<AssessmentOptionResponse> options;
}

