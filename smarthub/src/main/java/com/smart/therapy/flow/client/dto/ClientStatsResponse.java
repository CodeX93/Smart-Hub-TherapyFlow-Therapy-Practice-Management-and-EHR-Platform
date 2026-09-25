package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ClientStatsResponse {

    private Long totalClients;
    private Long activeClients;
    private Long pendingClients;
    private Long completedClients;
}

