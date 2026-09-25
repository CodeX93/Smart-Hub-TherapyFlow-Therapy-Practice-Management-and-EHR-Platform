package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LibraryConnectionBatchResponse {
    int total;
    int created;
    int skipped;
    @Singular
    List<LibraryConnectionResponse> createdConnections;
    @Singular("skippedConnection")
    List<SkippedConnection> skippedConnections;

    @Value
    @Builder
    public static class SkippedConnection {
        Long fromEntryId;
        Long toEntryId;
        String reason;
    }
}

