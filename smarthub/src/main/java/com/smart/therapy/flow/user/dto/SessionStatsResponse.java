package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Session statistics for a therapist")
public class SessionStatsResponse {

    @Schema(description = "Therapist ID", example = "1")
    private Long therapistId;

    @Schema(description = "Therapist name", example = "Dr. John Doe")
    private String therapistName;

    @Schema(description = "Total number of sessions", example = "200")
    private Long totalSessions;

    @Schema(description = "Number of sessions by status")
    private Map<String, Long> sessionsByStatus;

    @Schema(description = "Number of sessions by type")
    private Map<String, Long> sessionsByType;

    @Schema(description = "Average session duration in minutes", example = "55.5")
    private Double averageDuration;

    @Schema(description = "Total hours of sessions", example = "180.5")
    private Double totalHours;

    @Schema(description = "Number of unique clients", example = "45")
    private Long uniqueClients;

    @Schema(description = "Date range start (optional filter)", example = "2025-01-01T00:00:00Z")
    private Instant startDate;

    @Schema(description = "Date range end (optional filter)", example = "2025-12-31T23:59:59Z")
    private Instant endDate;
}
