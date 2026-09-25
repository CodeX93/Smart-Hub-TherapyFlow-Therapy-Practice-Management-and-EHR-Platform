package com.smart.therapy.flow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ReportSupportingFileResponse {
    private Long id;
    private Long clientId;
    private String originalName;
    private String mimeType;
    private Integer fileSize;
    private String documentType;
    private Long createdById;
    private Instant createdAt;
}
