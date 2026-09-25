package com.smart.therapy.flow.client.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BulkUploadRequest {
    private List<Map<String, Object>> clients;
}

