package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Map;

@Data
public class SubmitFormResponseRequest {
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    // Map of assignmentFieldId -> value
    // Note: In enterprise architecture, responses reference assignment field snapshots, not direct fields
    // This ensures historical data integrity when templates are updated
    @NotNull(message = "Responses are required")
    private Map<Long, String> responses;
}

