package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class SessionTranscriptResponse {
    private Long transcriptId;
    private Long sessionId;
    private Long clientId;
    private String clientName;
    private String uploadId;
    private String status;
    private String language;
    private Integer expectedChunks;
    private Integer receivedChunks;
    private String finalTranscript;
    private String diarizedTranscript; // AI speaker-labeled version; null until diarize is triggered
    private String content;
    private Integer durationSeconds;
    private Integer wordCount;
    private String failureReason;
    private Instant startedAt;
    private Instant finalizedAt;
    private Instant expiresAt;
    private Instant updatedAt;
    private List<SessionTranscriptChunkResponse> chunks;
}

