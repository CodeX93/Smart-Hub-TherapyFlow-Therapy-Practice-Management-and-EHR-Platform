package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@Schema(description = "Form assignment response")
public class FormAssignmentResponse {
    private Long id;
    private Long templateId;
    private Long templateVersionId; // Version used for this assignment
    private Long versionNumber; // Version number
    private String templateName;
    private String templateCategory;
    private Long clientId;
    private String clientName;
    private Long assignedById;
    private String assignedByName;
    private String status; // pending, in_progress, completed, expired
    private Instant dueDate;
    private String instructions;
    private Instant completedAt;
    private Instant submittedAt;
    private Instant reviewedAt;
    private Long reviewedById;
    private String reviewedByName;
    private String reviewNotes;
    private Integer remindersSent;
    private Instant lastReminderAt;
    private Instant createdAt;
    private Instant updatedAt;
    private List<FormResponseDto> responses;
    private List<FormSignatureResponse> signatures;
}

