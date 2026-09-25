package com.smart.therapy.flow.admin.controller;

import com.smart.therapy.flow.admin.dto.AdminIntegrationHealthResponse;
import com.smart.therapy.flow.admin.dto.IntegrationTestResponse;
import com.smart.therapy.flow.admin.service.AdminIntegrationHealthService;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/integrations")
@RequiredArgsConstructor
@Tag(name = "Admin Integration Health", description = "Organization integration health and test APIs for Stripe and therapist Zoom")
public class AdminIntegrationHealthController {

    private final AdminIntegrationHealthService adminIntegrationHealthService;

    @GetMapping("/health")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Get integration health",
            description = """
                    Returns organization-level Stripe health and therapist-level Zoom health.
                    Optionally includes therapist available slots for a date/service.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<AdminIntegrationHealthResponse> getIntegrationHealth(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Long serviceId,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) String timezone,
            @RequestParam(defaultValue = "false") boolean includeSlots,
            @RequestParam(defaultValue = "false") boolean runZoomLiveTest,
            @RequestParam(required = false) List<Long> therapistIds
    ) {
        return ResponseEntity.ok(adminIntegrationHealthService.getIntegrationHealth(
                date, serviceId, sessionType, timezone, includeSlots, runZoomLiveTest, therapistIds));
    }

    @GetMapping("/therapists/zoom-availability")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "List therapists Zoom status with available slots",
            description = """
                    Returns therapist list with zoomEnabled/configured status and available slots/timings.
                    `date` and `serviceId` are required for slot generation.
                    """,
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AdminIntegrationHealthResponse.TherapistZoomStatus>> getTherapistZoomAvailability(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam Long serviceId,
            @RequestParam(required = false) String sessionType,
            @RequestParam(required = false) String timezone,
            @RequestParam(defaultValue = "false") boolean runZoomLiveTest,
            @RequestParam(required = false) Collection<Long> therapistIds
    ) {
        return ResponseEntity.ok(adminIntegrationHealthService.getTherapistZoomAvailabilityList(
                date, serviceId, sessionType, timezone, runZoomLiveTest, therapistIds));
    }

    @PostMapping("/stripe/test")
    @PreAuthorize(RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.BILLING_MANAGE)
    @Operation(summary = "Test Stripe integration for current organization",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<IntegrationTestResponse> testStripeIntegration(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        Long actorAuthId = principal != null ? principal.getAuthId() : null;
        return ResponseEntity.ok(adminIntegrationHealthService.testStripeIntegration(actorAuthId));
    }

    @PostMapping("/zoom/test/{therapistId}")
    @PreAuthorize(StaffAuthorizationExpressions.CONSENT_ADMIN_MODULE_ACCESS)
    @Operation(summary = "Test Zoom integration for a therapist",
            security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<IntegrationTestResponse> testTherapistZoomIntegration(
            @PathVariable Long therapistId
    ) {
        return ResponseEntity.ok(adminIntegrationHealthService.testTherapistZoomIntegration(therapistId));
    }
}
