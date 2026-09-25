package com.smart.therapy.flow.report.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ClientReportResponse {
    private Long id;
    private Long clientId;
    private Long templateId;
    private String templateName;
    private String generatedContent;
    private String draftContent;
    private String finalContent;
    private Boolean isDraft;
    private Boolean isFinalized;
    private Instant generatedAt;
    private Instant editedAt;
    private Instant finalizedAt;
    private Long createdById;
    private String createdByName;
    private Long finalizedById;
    private ReportTemplateResponse template;
}
