package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SessionTranscriptChunkResponse {
    private Integer chunkIndex;
    private String chunkStatus;
    private String chunkText;
    private String failureReason;
    private Instant receivedAt;
}

