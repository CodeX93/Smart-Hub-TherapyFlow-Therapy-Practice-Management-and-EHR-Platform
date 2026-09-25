package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class SessionTranscriptStatusResponse {
    private Long transcriptId;
    private Long sessionId;
    private Long clientId;
    private String clientName;
    private String uploadId;
    private String status;
    private Integer expectedChunks;
    private Integer receivedChunks;
    private Instant startedAt;
    private Instant finalizedAt;
    private Instant updatedAt;
}

