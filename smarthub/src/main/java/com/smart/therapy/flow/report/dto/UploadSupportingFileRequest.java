package com.smart.therapy.flow.report.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UploadSupportingFileRequest {
    @NotBlank
    private String fileContent;
    @NotBlank
    private String originalName;
    @NotBlank
    private String mimeType;
    private String documentType;
    private Long templateId;
}
