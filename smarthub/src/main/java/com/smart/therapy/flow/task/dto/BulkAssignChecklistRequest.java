package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BulkAssignChecklistRequest {
    @NotNull(message = "Template ID is required")
    private Long templateId;
    
    @NotEmpty(message = "At least one client ID is required")
    private List<Long> clientIds;
    
    private LocalDate dueDate;
    private String description;
    private String notes;
}
