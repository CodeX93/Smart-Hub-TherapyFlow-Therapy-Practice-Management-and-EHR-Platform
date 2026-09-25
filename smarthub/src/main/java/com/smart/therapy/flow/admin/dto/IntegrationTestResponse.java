package com.smart.therapy.flow.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationTestResponse {
    private String integration;
    private Boolean success;
    private String message;
    private Instant testedAt;
    private Map<String, Object> details;
}
