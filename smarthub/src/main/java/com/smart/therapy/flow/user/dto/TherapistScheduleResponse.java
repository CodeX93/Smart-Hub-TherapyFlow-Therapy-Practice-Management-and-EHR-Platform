package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Therapist schedule for a specific date")
public class TherapistScheduleResponse {

    @Schema(description = "Therapist ID", example = "1")
    private Long therapistId;

    @Schema(description = "Therapist name", example = "Dr. John Doe")
    private String therapistName;

    @Schema(description = "Date for which schedule is retrieved", example = "2025-01-25")
    private LocalDate date;

    @Schema(description = "List of scheduled sessions for this date")
    private List<ScheduleSession> sessions;

    @Schema(description = "Total number of sessions scheduled for this date", example = "5")
    private Integer totalSessions;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Session information in schedule")
    public static class ScheduleSession {
        @Schema(description = "Session ID", example = "123")
        private Long sessionId;

        @Schema(description = "Client ID", example = "456")
        private Long clientId;

        @Schema(description = "Client name", example = "Jane Smith")
        private String clientName;

        @Schema(description = "Session start time", example = "2025-01-25T10:00:00Z")
        private Instant startTime;

        @Schema(description = "Session duration in minutes", example = "60")
        private Integer duration;

        @Schema(description = "Session type", example = "psychotherapy")
        private String sessionType;

        @Schema(description = "Session mode", example = "in-person")
        private String sessionMode;

        @Schema(description = "Session status", example = "scheduled")
        private String status;

        @Schema(description = "Room ID if applicable", example = "1")
        private Long roomId;

        @Schema(description = "Room name if applicable", example = "Therapy Room 101")
        private String roomName;
    }
}
