package com.smart.therapy.flow.user.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.user.dto.CreateTherapistBlockedTimeRequest;
import com.smart.therapy.flow.user.dto.TherapistBlockedTimeResponse;
import com.smart.therapy.flow.user.dto.AvailableSlotResponse;
import com.smart.therapy.flow.user.dto.BookingCountResponse;
import com.smart.therapy.flow.user.dto.SessionStatsResponse;
import com.smart.therapy.flow.user.dto.TherapistScheduleResponse;
import com.smart.therapy.flow.user.service.TherapistAvailabilityService;
import com.smart.therapy.flow.session.service.SessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/therapist-availability")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Therapist Availability", description = "APIs for managing therapist availability, blocked times, schedules, and statistics")
public class TherapistAvailabilityController {

    private final TherapistAvailabilityService therapistAvailabilityService;
    private final SessionService sessionService;

    @GetMapping("/therapist-blocked-times")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Get therapist blocked times",
            description = """
                    Retrieve all blocked times for a therapist.
                    
                    **Query Parameters:**
                    - `therapistId` (REQUIRED): ID of the therapist
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<TherapistBlockedTimeResponse>> getTherapistBlockedTimes(
            @Parameter(description = "Therapist ID (REQUIRED)", required = true, example = "123")
            @RequestParam("therapistId") Long therapistId
    ) {
        List<TherapistBlockedTimeResponse> blockedTimes = therapistAvailabilityService.getTherapistBlockedTimes(therapistId);
        return ResponseEntity.ok(blockedTimes);
    }

    @GetMapping("/therapist-blocked-times/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    public ResponseEntity<TherapistBlockedTimeResponse> getBlockedTime(@PathVariable("id") Long id) {
        TherapistBlockedTimeResponse blockedTime = therapistAvailabilityService.getBlockedTime(id);
        return ResponseEntity.ok(blockedTime);
    }

    @PostMapping("/therapist-blocked-times")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Create blocked time",
            description = """
                    Create a new blocked time for a therapist.
                    
                    **Request Body:**
                    - See CreateTherapistBlockedTimeRequest DTO for required/optional fields
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TherapistBlockedTimeResponse> createBlockedTime(
            @Valid @RequestBody CreateTherapistBlockedTimeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        TherapistBlockedTimeResponse blockedTime = therapistAvailabilityService.createBlockedTime(request, principal);
        return ResponseEntity.status(201).body(blockedTime);
    }

    @PatchMapping("/therapist-blocked-times/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    public ResponseEntity<TherapistBlockedTimeResponse> updateBlockedTime(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateTherapistBlockedTimeRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        TherapistBlockedTimeResponse blockedTime = therapistAvailabilityService.updateBlockedTime(id, request, principal);
        return ResponseEntity.ok(blockedTime);
    }

    @DeleteMapping("/therapist-blocked-times/{id}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    public ResponseEntity<Void> deleteBlockedTime(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        therapistAvailabilityService.deleteBlockedTime(id, principal);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/availability/slots")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Get available time slots",
            description = """
                    Get available time slots for a therapist on a specific date for a service.
                    
                     **Query Parameters:**
                     - `therapistId` (REQUIRED): ID of the therapist
                     - `date` (REQUIRED): Date in ISO format (yyyy-MM-dd), e.g., "2025-12-25"
                     - `serviceId` (REQUIRED): ID of the service
                     - `sessionType` (optional): `online` or `in-person`
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<List<AvailableSlotResponse>> getAvailableSlots(
            @Parameter(description = "Therapist ID (REQUIRED)", required = true, example = "123")
            @RequestParam("therapistId") Long therapistId,
            @Parameter(description = "Date in ISO format yyyy-MM-dd (REQUIRED)", required = true, example = "2025-12-25")
            @RequestParam("date") @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date,
            @Parameter(description = "Service ID (REQUIRED)", required = true, example = "456")
            @RequestParam("serviceId") Long serviceId,
            @Parameter(description = "Session type (optional): online or in-person", example = "online")
            @RequestParam(value = "sessionType", required = false) String sessionType
    ) {
        List<AvailableSlotResponse> slots = therapistAvailabilityService
                .getAvailableTimeSlots(therapistId, date, serviceId, null, sessionType);
        return ResponseEntity.ok(slots);
    }

    // ========== THERAPIST SCHEDULE AND STATISTICS ENDPOINTS ==========

    @GetMapping(value = "/api/v1/therapists/{therapistId}/schedule/{date}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Get therapist schedule for a date",
            description = """
                    Get the complete schedule for a therapist on a specific date.
                    
                    **Note:** This endpoint is for staff/therapist use only. Clients should use the client portal endpoints:
                    - `/api/v1/portal/appointments` - to view their own appointments
                    
                    **Path Parameters:**
                    - `therapistId` (REQUIRED): ID of the therapist (User entity)
                    - `date` (REQUIRED): Date in ISO format (yyyy-MM-dd), e.g., "2025-01-25"
                    
                    **Returns:** List of scheduled sessions for the therapist on the specified date
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TherapistScheduleResponse> getTherapistSchedule(
            @Parameter(description = "Therapist ID (REQUIRED) - User entity ID", required = true, example = "1")
            @PathVariable("therapistId") Long therapistId,
            @Parameter(description = "Date in ISO format yyyy-MM-dd (REQUIRED)", required = true, example = "2025-01-25")
            @PathVariable("date") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        TherapistScheduleResponse response = therapistAvailabilityService.getTherapistSchedule(therapistId, date);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/v1/therapists/{therapistId}/bookings/count")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Get therapist booking counts",
            description = """
                    Get booking count statistics for a therapist.
                    
                    **Path Parameters:**
                    - `therapistId` (REQUIRED): ID of the therapist (User entity)
                    
                    **Query Parameters:**
                    - `startDate` (optional): Start date for filtering (ISO 8601 format)
                    - `endDate` (optional): End date for filtering (ISO 8601 format)
                    
                    **Returns:** Booking counts (total, upcoming, completed, cancelled)
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<BookingCountResponse> getBookingCount(
            @Parameter(description = "Therapist ID (REQUIRED) - User entity ID", required = true, example = "1")
            @PathVariable("therapistId") Long therapistId,
            @Parameter(description = "Start date (optional, ISO 8601)", example = "2025-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End date (optional, ISO 8601)", example = "2025-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        BookingCountResponse response = sessionService.getTherapistBookingCount(therapistId, startDate, endDate);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/api/v1/therapists/{therapistId}/session-stats")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_ACCESS)
    @Operation(
            summary = "Get therapist session statistics",
            description = """
                    Get comprehensive session statistics for a therapist.
                    
                    **Path Parameters:**
                    - `therapistId` (REQUIRED): ID of the therapist (User entity)
                    
                    **Query Parameters:**
                    - `startDate` (optional): Start date for filtering (ISO 8601 format)
                    - `endDate` (optional): End date for filtering (ISO 8601 format)
                    
                    **Returns:** Session statistics including counts by status/type, average duration, total hours, unique clients
                    
                    **Requires:** ADMIN, SUPERVISOR, or THERAPIST role.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<SessionStatsResponse> getSessionStats(
            @Parameter(description = "Therapist ID (REQUIRED) - User entity ID", required = true, example = "1")
            @PathVariable("therapistId") Long therapistId,
            @Parameter(description = "Start date (optional, ISO 8601)", example = "2025-01-01T00:00:00Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startDate,
            @Parameter(description = "End date (optional, ISO 8601)", example = "2025-12-31T23:59:59Z")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endDate
    ) {
        SessionStatsResponse response = sessionService.getTherapistSessionStats(therapistId, startDate, endDate);
        return ResponseEntity.ok(response);
    }
}


