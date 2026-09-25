package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class BulkUploadSessionsResponse {
    private Integer total;
    private Integer successful;
    private Integer failed;
    private List<Map<String, Object>> errors;
}

