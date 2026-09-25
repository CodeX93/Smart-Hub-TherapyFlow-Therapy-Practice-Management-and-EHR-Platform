package com.smart.therapy.flow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ReportTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private String aiInstructions;
    private String originalName;
    private String mimeType;
    private Integer fileSize;
    private String structureText;
    private Boolean defaultIncludeProfile;
    private Boolean defaultIncludeNotes;
    private Boolean defaultIncludeAssessments;
    private String supportingFilesGuidance;
    private Boolean supportingFilesExpected;
    private List<String> supportingFileTypes;
    private Boolean isActive;
    private Long createdById;
    private Instant createdAt;
    private Instant updatedAt;
}
