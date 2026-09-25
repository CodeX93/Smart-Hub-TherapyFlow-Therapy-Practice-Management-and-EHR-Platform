package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to create a new therapy session")
public class CreateSessionRequest {

    @NotNull(message = "Client ID is required")
    @Schema(description = "ID of the client for this session", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;

    @NotNull(message = "Therapist ID is required")
    @Schema(description = "ID of the therapist conducting the session", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long therapistId;

    @NotNull(message = "Session date is required")
    @FutureOrPresent(message = "Session date cannot be in the past")
    @Schema(description = "Date and time of the session (ISO 8601 format)", example = "2025-12-25T14:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private Instant sessionDate;

    @NotNull(message = "Session mode is required")
    @Schema(description = "Session mode option key from system options (e.g. in_person, Online, phone, hybrid)", example = "in_person", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionMode;

    @Schema(description = "Clinical session type option key (e.g. assessment, individual)", example = "assessment")
    private String sessionType;

    @Schema(description = "Session status", example = "scheduled", defaultValue = "scheduled", allowableValues = {"scheduled", "confirmed", "completed", "cancelled", "no-show"})
    private String status;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours")
    @Schema(description = "Session duration in minutes", example = "60", defaultValue = "60", minimum = "15", maximum = "480")
    private Integer duration = 60;

    @Schema(description = "ID of the billing service for this session", example = "1")
    private Long serviceId;

    @Schema(description = "ID of the room where session will take place", example = "1")
    private Long roomId;

    @Schema(description = "Additional notes about the session", example = "First session - intake assessment")
    private String notes;

    @Schema(description = "Whether Zoom is enabled for this session", example = "false", defaultValue = "false")
    private Boolean zoomEnabled = false;

    @Schema(description = "Whether to ignore scheduling conflicts (use with caution)", example = "false", defaultValue = "false")
    private Boolean ignoreConflicts = false;
    
    /**
     * Optional timezone for the session date.
     * If not provided, will use therapist's timezone.
     * Should be IANA timezone ID (e.g., "America/New_York", "America/Los_Angeles").
     */
    @Schema(description = "IANA timezone ID for the session (e.g., 'America/New_York', 'America/Los_Angeles'). If not provided, uses therapist's timezone.", example = "America/New_York")
    private String timezone;
}

