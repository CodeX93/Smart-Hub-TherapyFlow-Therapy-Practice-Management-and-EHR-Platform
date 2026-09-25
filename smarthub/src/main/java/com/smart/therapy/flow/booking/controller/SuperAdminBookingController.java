package com.smart.therapy.flow.booking.controller;

import com.smart.therapy.flow.booking.dto.BookingRequestDto;
import com.smart.therapy.flow.booking.entity.BookingRequestStatus;
import com.smart.therapy.flow.booking.service.BookingRequestService;
import com.smart.therapy.flow.common.security.RoleConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/super-admin/booking-requests")
@RequiredArgsConstructor
@PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
public class SuperAdminBookingController {

    private final BookingRequestService service;

    @GetMapping
    public ResponseEntity<Page<BookingRequestDto>> getAllRequests(@PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(service.getAllRequests(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<BookingRequestDto> getRequestById(@PathVariable Long id) {
        return ResponseEntity.ok(service.getRequestById(id));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<BookingRequestDto> updateStatus(
            @PathVariable Long id, 
            @RequestParam BookingRequestStatus status,
            @RequestParam(required = false) String statusMessage) {
        return ResponseEntity.ok(service.updateStatus(id, status, statusMessage));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRequest(@PathVariable Long id) {
        service.deleteRequest(id);
        return ResponseEntity.noContent().build();
    }
}
