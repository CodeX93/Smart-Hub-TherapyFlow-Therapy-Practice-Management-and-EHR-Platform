package com.smart.therapy.flow.assessment.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request to create assessment reminders")
public class AssessmentReminderRequest {

    @NotNull(message = "Assignment ID is required")
    @Schema(description = "Assessment assignment ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long assignmentId;

    @Min(value = 1, message = "Reminder days must be at least 1")
    @Schema(description = "Number of days before due date to send reminder", example = "1", defaultValue = "1")
    private Integer reminderDaysBefore = 1;

    @Schema(description = "Custom reminder date (optional, overrides reminderDaysBefore)")
    private LocalDate reminderDate;

    @Schema(description = "Whether to send email reminder", example = "true", defaultValue = "true")
    private Boolean sendEmail = true;

    @Schema(description = "Whether to send in-app notification", example = "true", defaultValue = "true")
    private Boolean sendInApp = true;
}
