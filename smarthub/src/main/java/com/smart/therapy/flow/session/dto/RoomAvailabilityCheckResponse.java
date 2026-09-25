package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomAvailabilityCheckResponse {
    private boolean available;
    private String message;
    private List<ConflictInfo> roomConflicts;
    private List<ConflictInfo> therapistConflicts;
    private List<ConflictInfo> clientConflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictInfo {
        private Long sessionId;
        private Instant startTime;
        private Instant endTime;
        private String reason; // e.g., "Room occupied", "Therapist busy", "Client busy"
    }
}
