package com.smart.therapy.flow.assessment.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssessmentQuestionRequest {

    private String questionText;
    private String questionType; // text, multiple_choice, rating, scale, yes_no
    private Boolean isRequired = false;
    private Integer sortOrder = 0;
    private Double ratingMin;
    private Double ratingMax;
    private String ratingLabels;
    private Boolean contributesToScore = true;
    private List<AssessmentOptionRequest> options;
}

