package com.smart.therapy.flow.report.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ClientReportDownloadResult {
    byte[] content;
    String contentType;
    String filename;
}
