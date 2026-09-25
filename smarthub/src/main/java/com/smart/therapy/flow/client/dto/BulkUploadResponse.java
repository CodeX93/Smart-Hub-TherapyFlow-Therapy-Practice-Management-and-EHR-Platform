package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BulkUploadResponse {
    private Integer total;
    private Integer successful;
    private Integer failed;
    private List<Map<String, Object>> errors;
}

