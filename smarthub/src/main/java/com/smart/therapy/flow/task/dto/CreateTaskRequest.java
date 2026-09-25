package com.smart.therapy.flow.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to create a new task")
public class CreateTaskRequest {

    @NotBlank(message = "Title is required")
    @Schema(description = "Task title or custom text (REQUIRED unless titleKey is provided)", example = "Follow up with client", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "Predefined task title option key from task_titles catalog (optional)", example = "initial_assessment")
    private String titleKey;

    @Schema(description = "Task type option key from task_types catalog (optional)", example = "client_contact")
    private String taskType;

    @Schema(description = "Task description (optional)", example = "Check in on progress and schedule next session")
    private String description;

    @NotNull(message = "Priority is required")
    @Schema(description = "Task priority (REQUIRED)", example = "medium", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"low", "medium", "high", "urgent"})
    private String priority; // low, medium, high, urgent

    @NotNull(message = "Status is required")
    @Schema(description = "Task status (REQUIRED)", example = "pending", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"pending", "in_progress", "completed", "overdue"})
    private String status; // pending, in_progress, completed, overdue

    @NotNull(message = "Client ID is required")
    @Schema(description = "ID of the client this task is for (REQUIRED)", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;

    @Schema(description = "ID of the user assigned to this task (optional)", example = "1")
    private Long assignedToId;

    @Schema(description = "Due date for the task (optional, ISO 8601 format)", example = "2025-12-31T23:59:59Z", type = "string", format = "date-time")
    private Instant dueDate;
}

