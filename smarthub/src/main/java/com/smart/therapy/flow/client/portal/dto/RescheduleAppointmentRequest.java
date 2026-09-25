package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "Request to reschedule an appointment")
public class RescheduleAppointmentRequest {
    @NotBlank(message = "New session start time (UTC) is required")
    @Schema(description = "New session start time in UTC (REQUIRED, ISO 8601 format)", example = "2025-12-25T14:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private String newSessionStartUtc; // ISO 8601 format
    
    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours (480 minutes)")
    @Schema(description = "Session duration in minutes (OPTIONAL, default: uses existing duration). Must be between 15 and 480 minutes.", example = "60", minimum = "15", maximum = "480")
    private Integer duration; // Minutes, optional (will use existing duration if not provided)
}

