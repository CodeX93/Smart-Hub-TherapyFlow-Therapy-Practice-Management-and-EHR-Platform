package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class AssessmentAssignmentResponse {

    private Long id;
    private Long templateId;
    private String templateName;
    private Long clientId;
    private String clientName;
    private String status;
    /** Human-readable status for UI display (e.g. waiting_for_review → "Submitted"). */
    private String statusLabel;
    private Long assignedById;
    private String assignedByName;
    private Instant assignedDate;
    private Instant dueDate;
    private Instant completedAt;
    private Double totalScore;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

