package com.smart.therapy.flow.client.controller;

import com.smart.therapy.flow.client.dto.ClientFiltersBatchResponse;
import com.smart.therapy.flow.client.service.ClientFilterQueryService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/client-filters")
@RequiredArgsConstructor
@Slf4j
@io.swagger.v3.oas.annotations.tags.Tag(name = "Client Filters", description = "APIs for retrieving filter options for client management")
public class ClientFilterController {

    private final ClientFilterQueryService clientFilterQueryService;

    @GetMapping("/batch")
    @PreAuthorize("hasAuthority('CONSENT_ADMIN_VIEW')")
    @io.swagger.v3.oas.annotations.Operation(summary = "Get client filter options batch", description = """
            Retrieve all filter options needed for client management page in a single request.

            **Returns:**
            - `therapists`: List of available therapists (filtered by role)
            - `checklistTemplates`: List of checklist templates
            - `systemOptions`: System options for categories (client_type, referral_sources, marital_status, employment_status, education_level, gender, preferred_language)

            **Role-based filtering:**
            - ADMIN: All active therapists
            - SUPERVISOR: Only therapists supervised by this supervisor
            - THERAPIST: Only themselves

            **Requires:** ADMIN, SUPERVISOR, or THERAPIST role JWT token.
            """, security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"))
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Filter options retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<ClientFiltersBatchResponse> getClientFiltersBatch(
            @AuthenticationPrincipal AuthPrincipal principal) {
        return ResponseEntity.ok(clientFilterQueryService.getClientFiltersBatch(principal));
    }
}

