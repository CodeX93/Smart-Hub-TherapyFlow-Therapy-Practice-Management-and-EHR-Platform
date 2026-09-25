package com.smart.therapy.flow.report.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateReportTemplateRequest {
    @NotBlank
    private String name;
    private String description;
    private String aiInstructions;
    @NotBlank
    private String fileContent;
    @NotBlank
    private String originalName;
    @NotBlank
    private String mimeType;
    private Boolean defaultIncludeProfile;
    private Boolean defaultIncludeNotes;
    private Boolean defaultIncludeAssessments;
    private String supportingFilesGuidance;
    private Boolean supportingFilesExpected;
    private List<String> supportingFileTypes;
}
