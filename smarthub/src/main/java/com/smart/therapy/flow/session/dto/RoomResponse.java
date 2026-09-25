package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.smart.therapy.flow.session.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Room response")
public class RoomResponse {

    @Schema(description = "Room ID", example = "1")
    private Long id;

    @Schema(description = "Room number", example = "101")
    private String roomNumber;

    @Schema(description = "Room name", example = "Therapy Room 101")
    private String roomName;

    @Schema(description = "Room capacity", example = "4")
    private Integer capacity;

    @Schema(description = "Equipment description", example = "Whiteboard, comfortable chairs")
    private String equipment;

    @Schema(description = "Whether the room is active", example = "true")
    private Boolean isActive;

    @Schema(
            description = "Room type: PHYSICAL (in-office) or VIRTUAL (online)",
            example = "PHYSICAL",
            allowableValues = {"PHYSICAL", "VIRTUAL"}
    )
    private RoomType roomType;

    @Schema(description = "Created at", example = "2025-01-01T00:00:00Z")
    private Instant createdAt;

    @Schema(description = "Updated at", example = "2025-01-01T00:00:00Z")
    private Instant updatedAt;
}
