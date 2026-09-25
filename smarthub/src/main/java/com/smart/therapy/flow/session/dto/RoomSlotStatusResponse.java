package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomSlotStatusResponse {
    private Long roomId;
    private String roomNumber;
    private String roomName;
    private Boolean active;
    private String roomType;
    private Integer capacity;
    private Integer activeBookingsAtSlot;
    private Integer remainingCapacity;
    private Boolean full;
    private Boolean available;
    private Instant requestedStartTime;
    private Instant requestedEndTime;
    private Instant nextAvailableTime;
    private String message;
}
