package com.smart.therapy.flow.booking.controller;

import com.smart.therapy.flow.booking.dto.BookingRequestDto;
import com.smart.therapy.flow.booking.dto.CreateBookingRequestDto;
import com.smart.therapy.flow.booking.service.BookingRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/public/booking-requests")
@RequiredArgsConstructor
public class PublicBookingController {

    private final BookingRequestService service;

    @PostMapping
    public ResponseEntity<BookingRequestDto> createRequest(@Valid @RequestBody CreateBookingRequestDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.createRequest(dto));
    }
}
