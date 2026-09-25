package com.smart.therapy.flow.report.dto;

import lombok.Data;

import java.util.List;

@Data
public class UpdateReportTemplateRequest {
    private String name;
    private String description;
    private String aiInstructions;
    private String structureText;
    private Boolean isActive;
    private Boolean defaultIncludeProfile;
    private Boolean defaultIncludeNotes;
    private Boolean defaultIncludeAssessments;
    private String supportingFilesGuidance;
    private Boolean supportingFilesExpected;
    private List<String> supportingFileTypes;
}
