package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DuplicateRecommendation {
    private Long keepClientId;
    private Long deleteClientId;
    private List<String> reasons;
}

