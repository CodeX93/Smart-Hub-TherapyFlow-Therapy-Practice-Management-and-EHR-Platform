package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room availability response for public API")
public class RoomAvailabilityResponse {

    @Schema(description = "Date for which availability is checked", example = "2025-01-25")
    private LocalDate date;

    @Schema(description = "List of available rooms with their availability status")
    private List<RoomAvailabilityInfo> rooms;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "Room availability information")
    public static class RoomAvailabilityInfo {
        @Schema(description = "Room ID", example = "1")
        private Long roomId;

        @Schema(description = "Room number", example = "101")
        private String roomNumber;

        @Schema(description = "Room name", example = "Therapy Room 101")
        private String roomName;

        @Schema(description = "Whether the room is available on this date", example = "true")
        private Boolean available;

        @Schema(description = "List of available time slots for this room")
        private List<TimeSlot> availableSlots;

        @Schema(description = "List of booked time slots for this room")
        private List<TimeSlot> bookedSlots;
    }
}
