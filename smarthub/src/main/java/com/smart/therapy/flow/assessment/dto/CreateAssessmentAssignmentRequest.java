package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to assign an assessment to a client")
public class CreateAssessmentAssignmentRequest {

    @NotNull(message = "Template ID is required")
    @Schema(description = "ID of the assessment template (REQUIRED)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long templateId;

    @NotNull(message = "Client ID is required")
    @Schema(description = "ID of the client to assign the assessment to (REQUIRED)", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;

    @Schema(description = "Due date for the assessment (optional, ISO 8601 format)", example = "2025-12-31T23:59:59Z", type = "string", format = "date-time")
    private Instant dueDate;

    @Schema(description = "Additional notes about the assignment (optional)", example = "Please complete before next session")
    private String notes;
}

