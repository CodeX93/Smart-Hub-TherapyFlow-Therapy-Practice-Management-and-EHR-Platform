package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Supervisor assignment response")
public class SupervisorAssignmentResponse {

    @Schema(description = "Assignment ID", example = "1")
    private Long id;

    @Schema(description = "Supervisor user information")
    private SupervisorTherapistInfo supervisor;

    @Schema(description = "Therapist user information")
    private SupervisorTherapistInfo therapist;

    @Schema(description = "Type of assignment", example = "PRIMARY")
    private AssignmentType assignmentType;

    @Schema(description = "Start date of the assignment", example = "2026-01-27")
    private LocalDate startDate;

    @Schema(description = "End date of the assignment (null if ongoing)", example = "2026-12-31")
    private LocalDate endDate;

    @Schema(description = "Date when assignment was created", example = "2026-01-27T10:00:00Z")
    private Instant assignedDate;

    @Schema(description = "Whether the assignment is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Notes about the assignment", example = "Initial supervision assignment")
    private String notes;

    @Schema(description = "Required meeting frequency", example = "WEEKLY")
    private RequiredMeetingFrequency requiredMeetingFrequency;

    @Schema(description = "Next scheduled meeting date", example = "2026-02-03T10:00:00Z")
    private Instant nextMeetingDate;

    @Schema(description = "Last meeting date", example = "2026-01-27T10:00:00Z")
    private Instant lastMeetingDate;

    @Schema(description = "Created timestamp", example = "2026-01-27T10:00:00Z")
    private Instant createdAt;

    @Schema(description = "Updated timestamp", example = "2026-01-27T10:00:00Z")
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Supervisor or therapist information")
    public static class SupervisorTherapistInfo {
        @Schema(description = "User ID", example = "1")
        private Long id;

        @Schema(description = "Username", example = "john.doe")
        private String username;

        @Schema(description = "Full name", example = "John Doe")
        private String fullName;

        @Schema(description = "Email", example = "john.doe@therapyflow.pro")
        private String email;
    }
}
