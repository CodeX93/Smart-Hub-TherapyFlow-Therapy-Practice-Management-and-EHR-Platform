package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscribeFinalizeResponse {
    private Long id;
    private Long sessionId;
    private Long clientId;
    private String content;
    private String diarizedTranscript;
    private String status;
    private String language;
    private Integer durationSeconds;
    private Integer wordCount;
    private String uploadId;
}
