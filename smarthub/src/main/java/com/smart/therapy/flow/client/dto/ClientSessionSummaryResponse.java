package com.smart.therapy.flow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSessionSummaryResponse {
    private Long clientId;
    private int totalSessions;
    private int completed;
    private int scheduled;
    private int missedCancelled;
    private int conflicts;
}
