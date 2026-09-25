package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BulkOperationResponse {
    private Integer total;
    private Integer successful;
    private Integer failed;
    private Integer skipped; // For operations that may skip items
    private List<Map<String, Object>> errors;
    private Map<Long, Integer> distribution; // For therapist reassignment
}

