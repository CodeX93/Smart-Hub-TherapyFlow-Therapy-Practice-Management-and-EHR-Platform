package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TranscriptSmartFillResponse {
    private Long sessionId;
    private String uploadId;
    private String transcript;
    private Map<String, String> mappedFields;
    /**
     * Present when the transcript was processed but no note fields could be extracted.
     */
    private String message;
}

