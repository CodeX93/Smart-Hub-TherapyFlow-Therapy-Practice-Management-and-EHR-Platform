package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AssessmentTemplateResponse {

    private Long id;
    private String name;
    private String description;
    private String category;
    private Boolean isStandardized;
    private Integer version;
    private Boolean isActive;
    private List<AssessmentSectionResponse> sections;
    private Instant createdAt;
    private Instant updatedAt;
}

