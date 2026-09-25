package com.smart.therapy.flow.user.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.user.dto.TherapistDashboardSummaryResponse;
import com.smart.therapy.flow.user.service.TherapistDashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/therapists/dashboard")
@RequiredArgsConstructor
@Tag(name = "Therapist Dashboard", description = "Therapist-facing dashboard card metrics")
public class TherapistDashboardController {

    private final TherapistDashboardService therapistDashboardService;

    @GetMapping("/summary")
    @PreAuthorize("(" + RoleConstants.ROLE_THERAPIST + " or " + RoleConstants.ROLE_SUPERVISOR + ") and "
            + PermissionConstants.CLIENT_VIEW + " and "
            + PermissionConstants.SESSION_VIEW + " and "
            + PermissionConstants.BILLING_VIEW)
    @Operation(
            summary = "Get therapist dashboard summary",
            description = """
                    Returns therapist-scoped dashboard card metrics in the same shape as the admin summary,
                    but limited to the authenticated therapist's assigned clients, sessions, tasks, and billing.
                    Admins should use GET /api/v1/admin/dashboard/summary instead.
                    """,
            security = @SecurityRequirement(name = "BearerAuth")
    )
    public ResponseEntity<TherapistDashboardSummaryResponse> getDashboardSummary(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(therapistDashboardService.getDashboardSummary(principal));
    }
}
