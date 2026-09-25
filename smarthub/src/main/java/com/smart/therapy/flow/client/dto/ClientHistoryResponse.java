package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ClientHistoryResponse {
    private Long id;
    private Long clientId;
    private String eventType;
    private String eventSource;
    private String fromValue;
    private String toValue;
    private String description;
    private String changeSummary;
    private Long createdByUserId;
    private String createdByName;
    private Instant createdAt;
}

