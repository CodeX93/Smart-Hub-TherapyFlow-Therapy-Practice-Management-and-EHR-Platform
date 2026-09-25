package com.smart.therapy.flow.client.portal.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "Request to book an appointment/session")
public class BookAppointmentRequest {
    @NotNull(message = "Session start time (UTC) is required")
    @Schema(description = "Session start time in UTC (REQUIRED, ISO 8601 format)", example = "2025-12-25T14:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private String sessionStartUtc; // ISO 8601 format
    
    @Schema(description = "Session duration in minutes (optional, will use service duration if not provided)", example = "60")
    private Integer duration; // Minutes, optional (will use service duration if not provided)
    
    @NotNull(message = "Service ID is required")
    @Schema(description = "ID of the billing service (REQUIRED)", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long serviceId;
    
    @NotNull(message = "Session mode is required")
    @Schema(description = "Session mode option key (REQUIRED)", example = "in_person", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sessionType;
    
    @Schema(description = "Optional location notes for in-person sessions", example = "Main office - Room 101")
    private String location; // Optional location notes
}

