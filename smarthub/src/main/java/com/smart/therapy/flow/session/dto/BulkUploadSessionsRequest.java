package com.smart.therapy.flow.session.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class BulkUploadSessionsRequest {
    @NotEmpty
    private List<Map<String, Object>> sessions;
}

