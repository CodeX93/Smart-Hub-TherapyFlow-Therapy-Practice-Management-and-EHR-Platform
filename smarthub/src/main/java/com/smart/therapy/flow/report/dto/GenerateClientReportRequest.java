package com.smart.therapy.flow.report.dto;

import lombok.Data;

import java.util.List;

@Data
public class GenerateClientReportRequest {
    private Long templateId;
    private ReportSources sources;
    private List<Long> supportingFileIds;

    @Data
    public static class ReportSources {
        private Boolean includeProfile;
        private Boolean includeNotes;
        private Boolean includeAssessments;
    }
}
