package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewFormAssignmentRequest {
    @NotNull(message = "Assignment ID is required")
    private Long assignmentId;
    
    private String reviewNotes;
    
    private Boolean approved; // true = approved, false = needs revision
}

