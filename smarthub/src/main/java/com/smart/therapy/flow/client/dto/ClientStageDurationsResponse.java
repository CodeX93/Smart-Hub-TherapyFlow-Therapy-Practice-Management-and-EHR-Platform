package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
public class ClientStageDurationsResponse {
    private Long clientId;
    private String currentStage;
    private Map<String, Long> durations;
    private int totalEvents;
}
