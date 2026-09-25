package com.smart.therapy.flow.consultation.controller;

import com.smart.therapy.flow.consultation.dto.PublicBookConsultationRequest;
import com.smart.therapy.flow.consultation.dto.PublicBookConsultationResponse;
import com.smart.therapy.flow.consultation.dto.PublicTherapistResponse;
import com.smart.therapy.flow.consultation.service.PublicConsultationService;
import com.smart.therapy.flow.publicsite.dto.PublicSiteServiceResponse;
import com.smart.therapy.flow.user.dto.AvailableSlotResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/public/orgs/{orgSlug}")
@RequiredArgsConstructor
@Tag(name = "Public Consultation", description = "Public consultation therapists, slots, and booking")
public class PublicConsultationController {

    private final PublicConsultationService publicConsultationService;

    @GetMapping("/public-services")
    @Operation(summary = "List enabled public counseling services for the marketing site")
    public ResponseEntity<List<PublicSiteServiceResponse>> listPublicServices(@PathVariable String orgSlug) {
        return ResponseEntity.ok(publicConsultationService.listPublicServices(orgSlug));
    }

    @GetMapping("/therapists")
    @Operation(summary = "List therapists for an organisation (public)")
    public ResponseEntity<List<PublicTherapistResponse>> listTherapists(@PathVariable String orgSlug) {
        return ResponseEntity.ok(publicConsultationService.listTherapists(orgSlug));
    }

    @GetMapping("/therapists/{therapistId}/availability")
    @Operation(summary = "Available consultation slots for a therapist")
    public ResponseEntity<List<AvailableSlotResponse>> availability(
            @PathVariable String orgSlug,
            @PathVariable Long therapistId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "CONSULTATION") String serviceCode,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) Long publicServiceId) {
        return ResponseEntity.ok(publicConsultationService.getAvailability(
                orgSlug, therapistId, date, serviceCode, sessionType, publicServiceId));
    }

    @PostMapping("/consultations")
    @Operation(summary = "Book a public consultation session")
    public ResponseEntity<PublicBookConsultationResponse> book(
            @PathVariable String orgSlug,
            @Valid @RequestBody PublicBookConsultationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(publicConsultationService.book(orgSlug, request));
    }
}
