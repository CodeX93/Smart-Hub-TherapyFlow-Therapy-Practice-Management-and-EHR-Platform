package com.smart.therapy.flow.assessment.dto;

import lombok.Data;

import java.util.List;

@Data
public class AssessmentSectionRequest {

    private String title;
    private String description;
    private String accessLevel;
    private Boolean isScoring = false;
    private Integer sortOrder = 0;

    // Optional mapping into AI report sections and prompts
    private String reportMapping;
    private String aiReportPrompt;

    private List<AssessmentQuestionRequest> questions;
}

