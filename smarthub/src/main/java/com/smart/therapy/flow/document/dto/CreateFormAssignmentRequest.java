package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateFormAssignmentRequest {
    @NotNull(message = "Template ID is required")
    private Long templateId;
    
    @NotNull(message = "Client ID is required")
    private Long clientId;
    
    private Instant dueDate;
    
    private String instructions; // Custom instructions from therapist
}

