package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AssignChecklistRequest {
    @NotNull(message = "Template ID is required")
    private Long templateId;
    
    private LocalDate dueDate;
}

