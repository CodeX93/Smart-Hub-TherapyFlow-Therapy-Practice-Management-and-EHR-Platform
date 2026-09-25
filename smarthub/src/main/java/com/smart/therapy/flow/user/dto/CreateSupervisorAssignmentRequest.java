package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request to assign a supervisor to a therapist")
public class CreateSupervisorAssignmentRequest {

    @NotNull(message = "Supervisor ID is required")
    @Schema(description = "ID of the supervisor user (REQUIRED)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long supervisorId;

    @NotNull(message = "Therapist ID is required")
    @Schema(description = "ID of the therapist user (REQUIRED)", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long therapistId;

    @Schema(description = "Type of assignment (optional, default: PRIMARY)", example = "PRIMARY", allowableValues = {"PRIMARY", "SECONDARY", "CLINICAL"})
    private AssignmentType assignmentType;

    @Schema(description = "Start date of the assignment (optional, default: today)", example = "2026-01-27")
    private LocalDate startDate;

    @Schema(description = "End date of the assignment (optional, null means ongoing)", example = "2026-12-31")
    private LocalDate endDate;

    @NotNull(message = "Required meeting frequency is required")
    @Schema(description = "Required meeting frequency (REQUIRED)", example = "WEEKLY", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "YEARLY"})
    private RequiredMeetingFrequency requiredMeetingFrequency;

    @Schema(description = "Notes about the assignment (optional)", example = "Initial supervision assignment for new therapist")
    private String notes;
}
