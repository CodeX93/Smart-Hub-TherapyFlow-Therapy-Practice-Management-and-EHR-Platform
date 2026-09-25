package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Schema(description = "Request to update a supervisor assignment. All fields are optional - only include fields you want to update.")
public class UpdateSupervisorAssignmentRequest {

    @Schema(description = "Type of assignment (optional)", example = "PRIMARY", allowableValues = {"PRIMARY", "SECONDARY", "CLINICAL"})
    private AssignmentType assignmentType;

    @Schema(description = "Start date of the assignment (optional)", example = "2026-01-27")
    private LocalDate startDate;

    @Schema(description = "End date of the assignment (optional, null means ongoing)", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "Active flag (optional). If provided, the system will align assignment dates accordingly.", example = "true")
    private Boolean isActive;

    @Schema(description = "Required meeting frequency (optional)", example = "WEEKLY", allowableValues = {"DAILY", "WEEKLY", "BIWEEKLY", "MONTHLY", "YEARLY"})
    private RequiredMeetingFrequency requiredMeetingFrequency;

    @Schema(description = "Notes about the assignment (optional)", example = "Updated supervision notes")
    private String notes;

    @Schema(description = "Next planned supervision meeting date/time (optional, ISO-8601)", example = "2026-05-01T10:00:00Z")
    private Instant nextMeetingDate;

    @Schema(description = "Last completed supervision meeting date/time (optional, ISO-8601)", example = "2026-04-20T10:00:00Z")
    private Instant lastMeetingDate;
}
