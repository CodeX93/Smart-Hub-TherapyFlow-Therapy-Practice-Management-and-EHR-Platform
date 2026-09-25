package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class TranscribeAudioResponse {
    private Boolean success;
    private String rawTranscription;
    private Map<String, String> mappedFields;
    private Double qualityScore; // Transcription quality score (0.0-1.0)
    private String message; // Optional status message
}

