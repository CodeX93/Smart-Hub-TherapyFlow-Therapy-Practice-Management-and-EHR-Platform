package com.smart.therapy.flow.session.controller;

import com.smart.therapy.flow.common.util.HttpRequestUtil;
import com.smart.therapy.flow.session.dto.RoomRequest;
import com.smart.therapy.flow.session.dto.RoomResponse;
import com.smart.therapy.flow.session.dto.RoomSlotStatusResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.session.service.RoomService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import com.smart.therapy.flow.common.security.PermissionConstants;

import java.util.List;

@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Room Management", description = "Room management operations")
public class RoomController {

        private final RoomService roomService;

        @GetMapping
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Get all rooms", description = """
                        Get a list of all rooms.

                        **Query Parameters:**
                        - `activeOnly` (optional): If true, returns only active rooms

                        **Returns:** List of rooms

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<RoomResponse>> getRooms(
                        @RequestParam(required = false) Boolean activeOnly) {
                List<RoomResponse> rooms = roomService.getRooms(activeOnly);
                return ResponseEntity.ok(rooms);
        }

        @GetMapping("/{roomId}")
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Get room by ID", description = """
                        Get a specific room by its ID.

                        **Returns:** Room details

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<RoomResponse> getRoom(@PathVariable Long roomId) {
                RoomResponse room = roomService.getRoom(roomId);
                return ResponseEntity.ok(room);
        }

        @PostMapping
        @PreAuthorize(PermissionConstants.ROOM_MANAGE)
        @Operation(summary = "Create a new room", description = """
                        Create a new room.

                        **Validation:**
                        - `roomNumber`: Required, unique, max 50 characters, alphanumeric with spaces/hyphens/underscores
                        - `roomName`: Required, unique, max 255 characters
                        - `capacity`: Optional, must be positive
                        - `equipment`: Optional, max 1000 characters
                        - `isActive`: Required, boolean

                        **Requires:** ADMIN role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<RoomResponse> createRoom(
                        @Valid @RequestBody RoomRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                RoomResponse room = roomService.createRoom(
                                request, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(room);
        }

        @PutMapping("/{roomId}")
        @PreAuthorize(PermissionConstants.ROOM_MANAGE)
        @Operation(summary = "Update a room", description = """
                        Update an existing room. All fields are optional - only include fields you want to update.

                        **Validation:** Same as create, but all fields are optional

                        **Requires:** ADMIN role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<RoomResponse> updateRoom(
                        @PathVariable Long roomId,
                        @Valid @RequestBody RoomRequest request,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                RoomResponse room = roomService.updateRoom(
                                roomId, request, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.ok(room);
        }

        @DeleteMapping("/{roomId}")
        @PreAuthorize(PermissionConstants.ROOM_MANAGE)
        @Operation(summary = "Delete a room", description = """
                        Delete a room. Cannot delete if the room is linked to sessions (including archived
                        or recurring sessions), therapist room assignments, or room bookings.

                        **Requires:** ADMIN role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<Void> deleteRoom(
                        @PathVariable Long roomId,
                        @AuthenticationPrincipal AuthPrincipal principal,
                        HttpServletRequest httpRequest) {
                roomService.deleteRoom(roomId, principal, HttpRequestUtil.getClientIp(httpRequest));
                return ResponseEntity.noContent().build();
        }

        @GetMapping("/{roomId}/availability")
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Check room availability", description = """
                        Check if a room is available for a given time period.

                        **Query Parameters:**
                        - `startTime`: Start time (ISO 8601 format)
                        - `endTime`: End time (ISO 8601 format)

                        **Returns:** true if available, false otherwise

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<Boolean> checkRoomAvailability(
                        @PathVariable Long roomId,
                        @RequestParam java.time.Instant startTime,
                        @RequestParam java.time.Instant endTime) {
                boolean available = roomService.isRoomAvailable(roomId, startTime, endTime);
                return ResponseEntity.ok(available);
        }

        @GetMapping("/check-availability")
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Check detailed availability", description = """
                        Check availability for a room, therapist, and client for a specific time slot.
                        Returns detailed conflict information if any exist.

                        **Query Parameters:**
                        - `roomId` (optional): Room ID to check
                        - `therapistId` (optional): Therapist ID to check
                        - `clientId` (optional): Client ID to check
                        - `startTime`: Start time (ISO 8601 format)
                        - `endTime`: End time (ISO 8601 format)

                        **Returns:** Detailed availability status with conflicts

                        **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse> checkAvailability(
                        @RequestParam(required = false) Long roomId,
                        @RequestParam(required = false) Long therapistId,
                        @RequestParam(required = false) Long clientId,
                        @RequestParam java.time.Instant startTime,
                        @RequestParam java.time.Instant endTime) {
                com.smart.therapy.flow.session.dto.RoomAvailabilityCheckResponse response = roomService
                                .checkDetailedAvailability(
                                                roomId, startTime, endTime, therapistId, clientId);
                return ResponseEntity.ok(response);
        }

        @GetMapping("/available")
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Get available rooms for an exact slot", description = """
                        Returns available rooms for a therapist at a specific date/time slot.

                        **Query Parameters:**
                        - `therapistId` (required)
                        - `sessionDate` (required, ISO 8601 UTC)
                        - `sessionType` (required): `online` or `in-person`
                        - `serviceId` (optional): if provided, service duration is used
                        - `duration` (optional): fallback duration in minutes (default 60)
                        - `excludeSessionId` (optional): when editing a session, exclude it from
                          occupancy so its current room remains available

                        **Rule:** Rooms occupied by `CANCELLED` or `COMPLETED` sessions are considered available.
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<List<RoomResponse>> getAvailableRoomsForSlot(
                        @RequestParam Long therapistId,
                        @RequestParam java.time.Instant sessionDate,
                        @RequestParam String sessionType,
                        @RequestParam(required = false) Long serviceId,
                        @RequestParam(required = false) Integer duration,
                        @RequestParam(required = false) Long excludeSessionId) {
                List<RoomResponse> rooms = roomService.getAvailableRoomsForSlot(
                                therapistId, sessionDate, duration, serviceId, sessionType, excludeSessionId);
                return ResponseEntity.ok(rooms);
        }

        @GetMapping("/{roomId}/slot-status")
        @PreAuthorize(PermissionConstants.ROOM_READ_ACCESS)
        @Operation(summary = "Get room capacity status for selected date/time", description = """
                        Returns capacity and availability details for a room at a selected date/time.

                        **Query Parameters:**
                        - `sessionDate` (required, ISO 8601 UTC)
                        - `sessionType` (required): `online` or `in-person`
                        - `serviceId` (optional): if provided, service duration is used
                        - `duration` (optional): fallback duration in minutes (default 60)

                        **Returns:**
                        - full / available
                        - activeBookingsAtSlot / remainingCapacity / capacity
                        - nextAvailableTime when room is full
                        """, security = @SecurityRequirement(name = "BearerAuth"))
        public ResponseEntity<RoomSlotStatusResponse> getRoomSlotStatus(
                        @PathVariable Long roomId,
                        @RequestParam java.time.Instant sessionDate,
                        @RequestParam String sessionType,
                        @RequestParam(required = false) Long serviceId,
                        @RequestParam(required = false) Integer duration) {
                RoomSlotStatusResponse response = roomService.getRoomSlotStatus(
                                roomId, sessionDate, duration, serviceId, sessionType);
                return ResponseEntity.ok(response);
        }
}

