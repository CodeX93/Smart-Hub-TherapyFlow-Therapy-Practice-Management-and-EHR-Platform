package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
public class BulkAssignFormRequest {
    @NotNull(message = "Template ID is required")
    private Long templateId;
    
    @NotEmpty(message = "At least one client ID is required")
    private List<Long> clientIds;
    
    private Instant dueDate;
    
    private String instructions; // Custom instructions from therapist
}
