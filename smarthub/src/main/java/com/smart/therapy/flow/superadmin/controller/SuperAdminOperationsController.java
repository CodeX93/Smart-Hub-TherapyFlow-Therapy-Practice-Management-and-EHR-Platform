package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminBackupJobResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminBulkActionRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminForcePasswordResetRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminSuspendRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminTerminateRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformBackupJob;
import com.smart.therapy.flow.superadmin.service.SuperAdminNotificationService;
import com.smart.therapy.flow.superadmin.service.SuperAdminOperationsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin Operations", description = "Tenant lifecycle, backups, bulk actions, scheduled jobs")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminOperationsController {

    private final SuperAdminOperationsService operationsService;
    private final SuperAdminNotificationService notificationService;

    @PostMapping("/organisations/{id}/suspend")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Suspend organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> suspend(
            @PathVariable Long id,
            @RequestBody(required = false) SuperAdminSuspendRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var org = operationsService.suspendOrganisation(id, principal != null ? principal.getAuthId() : null,
                request != null ? request.getReason() : null);
        return ResponseEntity.ok(Map.of("organisationId", org.getId(), "status", org.getStatus()));
    }

    @PostMapping("/organisations/{id}/reactivate")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Reactivate organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> reactivate(
            @PathVariable Long id,
            @RequestBody(required = false) SuperAdminSuspendRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var org = operationsService.reactivateOrganisation(
                id,
                principal != null ? principal.getAuthId() : null,
                request != null ? request.getReason() : null
        );
        return ResponseEntity.ok(Map.of("organisationId", org.getId(), "status", org.getStatus()));
    }

    @PostMapping("/organisations/{id}/terminate")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Terminate organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> terminate(
            @PathVariable Long id,
            @Valid @RequestBody SuperAdminTerminateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var org = operationsService.terminateOrganisation(
                id,
                principal != null ? principal.getAuthId() : null,
                request.getReason(),
                request.getRetentionDays()
        );
        return ResponseEntity.ok(Map.of(
                "organisationId", org.getId(),
                "status", org.getStatus(),
                "effectiveAt", org.getTerminationEffectiveAt()
        ));
    }

    @PostMapping("/bulk/actions")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Run bulk actions for organisations", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> bulk(
            @Valid @RequestBody SuperAdminBulkActionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var result = operationsService.applyBulkAction(
                request.getOrganisationIds(),
                request.getAction(),
                principal != null ? principal.getAuthId() : null,
                request.getReason()
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/organisations/{id}/force-password-reset")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Force organisation password reset", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> forcePasswordReset(
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SuperAdminForcePasswordResetRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        var result = operationsService.forceOrganisationPasswordReset(
                id,
                request != null ? request.getEffectiveAt() : null,
                principal != null ? principal.getAuthId() : null,
                request != null ? request.getReason() : null
        );
        return ResponseEntity.ok(Map.of(
                "organisationId", id,
                "status", result.status(),
                "effectiveAt", result.effectiveAt(),
                "jobId", result.jobId() != null ? result.jobId() : "",
                "affectedUsers", result.affectedUsers(),
                "skippedUsers", result.skippedUsers()
        ));
    }

    @GetMapping("/organisations/{id}/backups")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List organisation backup jobs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminBackupJobResponse>> backups(@PathVariable Long id) {
        return ResponseEntity.ok(operationsService.listBackupJobs(id).stream().map(this::toBackupJobResponse).toList());
    }

    @GetMapping("/jobs/scheduled")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Scheduled/queued jobs summary", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> jobs() {
        return ResponseEntity.ok(Map.of(
                "backupJobs", operationsService.listBackupJobs(null).stream().map(this::toBackupJobResponse).toList(),
                "notificationJobs", notificationService.getHistory()
        ));
    }

    private SuperAdminBackupJobResponse toBackupJobResponse(PlatformBackupJob job) {
        SuperAdminBackupJobResponse row = new SuperAdminBackupJobResponse();
        row.setJobId(job.getId());
        row.setOrganisationId(job.getOrganisation().getId());
        row.setRequestedByAuthId(job.getRequestedByAuthId());
        row.setStatus(job.getStatus());
        row.setStorageLocation(job.getStorageLocation());
        row.setErrorMessage(job.getErrorMessage());
        row.setStartedAt(job.getStartedAt());
        row.setCompletedAt(job.getCompletedAt());
        row.setCreatedAt(job.getCreatedAt());
        return row;
    }

}
