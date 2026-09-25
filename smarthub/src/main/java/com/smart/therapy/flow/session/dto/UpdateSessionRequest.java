package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to update an existing session. All fields are optional - only include fields you want to update.")
public class UpdateSessionRequest {

    @Schema(description = "ID of the client for this session (optional)", example = "123")
    private Long clientId;

    @Schema(description = "ID of the therapist conducting the session (optional)", example = "1")
    private Long therapistId;

    @Schema(description = "Date and time of the session (optional, ISO 8601 format)", example = "2025-12-25T14:00:00Z", type = "string", format = "date-time")
    private Instant sessionDate;

    @Schema(description = "Session mode option key (optional)", example = "in_person")
    private String sessionMode;

    @Schema(description = "Clinical session type option key (optional)", example = "assessment")
    private String sessionType;

    @Schema(description = "Session status (optional)", example = "scheduled", allowableValues = {"scheduled", "confirmed", "completed", "cancelled", "no-show"})
    private String status;

    @Min(value = 15, message = "Duration must be at least 15 minutes")
    @Max(value = 480, message = "Duration cannot exceed 8 hours")
    @Schema(description = "Session duration in minutes (optional)", example = "60", minimum = "15", maximum = "480")
    private Integer duration;

    @Schema(description = "ID of the billing service for this session (optional)", example = "1")
    private Long serviceId;

    @Schema(description = "ID of the room where session will take place (optional)", example = "1")
    private Long roomId;

    @Schema(description = "Additional notes about the session (optional)", example = "First session - intake assessment")
    private String notes;

    @Schema(description = "Whether Zoom is enabled for this session (optional)", example = "false")
    private Boolean zoomEnabled;

    @Schema(description = "IANA timezone ID for update context (optional, e.g., 'Asia/Karachi', 'America/New_York')", example = "Asia/Karachi")
    private String timezone;

    @Schema(description = "Whether to ignore scheduling conflicts (optional, use with caution)", example = "false", defaultValue = "false")
    private Boolean ignoreConflicts = false;
}

