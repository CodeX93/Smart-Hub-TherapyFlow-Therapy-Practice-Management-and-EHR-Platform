package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AssessmentSectionResponse {

    private Long id;
    private String title;
    private String description;
    private String accessLevel;
    private Boolean isScoring;
    private String reportMapping;
    private String aiReportPrompt;
    private Integer sortOrder;
    private List<AssessmentQuestionResponse> questions;
}

