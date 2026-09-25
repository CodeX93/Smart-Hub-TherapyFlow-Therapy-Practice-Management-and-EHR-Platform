package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.smart.therapy.flow.session.enums.RoomType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "Request to create or update a room")
public class RoomRequest {

    @NotBlank(message = "Room number is required")
    @Size(max = 50, message = "Room number cannot exceed 50 characters")
    @Pattern(
            regexp = "^[a-zA-Z0-9\\s_-]+$",
            message = "Room number must contain only letters, numbers, spaces, hyphens, and underscores")
    @Schema(description = "Unique room number/identifier", example = "101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roomNumber;

    @NotBlank(message = "Room name is required")
    @Size(max = 255, message = "Room name cannot exceed 255 characters")
    @Schema(description = "Room name", example = "Therapy Room 101", requiredMode = Schema.RequiredMode.REQUIRED)
    private String roomName;

    @Positive(message = "Capacity must be a positive number")
    @Max(value = 1000, message = "Capacity cannot exceed 1000")
    @Schema(description = "Room capacity (number of people, max 1000)", example = "4")
    private Integer capacity;

    @Size(max = 1000, message = "Equipment description cannot exceed 1000 characters")
    @Schema(description = "Equipment description", example = "Whiteboard, comfortable chairs, privacy screen")
    private String equipment;

    @NotNull(message = "Active status is required")
    @Schema(description = "Whether the room is active", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean isActive;

    @NotNull(message = "Room type is required")
    @Schema(
            description = "Type of room: physical (in-office) or virtual (online)",
            example = "PHYSICAL",
            allowableValues = {"PHYSICAL", "VIRTUAL"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private RoomType roomType;
}
