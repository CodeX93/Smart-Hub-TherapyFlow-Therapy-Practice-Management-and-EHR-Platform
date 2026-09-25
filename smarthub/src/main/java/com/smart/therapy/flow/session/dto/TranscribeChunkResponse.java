package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscribeChunkResponse {
    private String uploadId;
    private Integer chunkIndex;
    private String chunkText;
    private Integer chunksReceived;
}
