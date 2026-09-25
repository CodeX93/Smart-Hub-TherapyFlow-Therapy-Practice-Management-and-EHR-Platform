package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DuplicateGroupResponse {
    private List<ClientResponse> clients;
    private String confidenceLevel; // high, medium
    private Integer confidenceScore;
    private String matchType;
    private DuplicateRecommendation recommendation;
}

