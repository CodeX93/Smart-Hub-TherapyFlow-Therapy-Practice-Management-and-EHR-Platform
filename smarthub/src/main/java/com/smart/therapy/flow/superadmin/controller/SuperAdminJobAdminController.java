package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.superadmin.dto.SuperAdminJobResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminJobRunResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminJobToggleRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminJobAdminService;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
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

@RestController
@RequestMapping("/api/v1/super-admin/jobs")
@RequiredArgsConstructor
@Tag(name = "Super Admin Jobs", description = "Maintenance and reporting scheduled jobs")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminJobAdminController {

    private final SuperAdminJobAdminService jobAdminService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List scheduled jobs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminJobResponse>> listJobs() {
        return ResponseEntity.ok(jobAdminService.listJobs());
    }

    @PostMapping("/{jobKey}/run")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Run a job immediately", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminJobRunResponse> runJob(
            @PathVariable String jobKey,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(jobAdminService.runNow(jobKey, principal != null ? principal.getAuthId() : null));
    }

    @PostMapping("/run-all")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Run all jobs immediately", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminJobRunResponse>> runAllJobs(
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(jobAdminService.runAllNow(principal != null ? principal.getAuthId() : null));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Scheduled job audit logs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> auditLogs(
            @RequestParam(required = false) String jobKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(jobAdminService.listJobAuditLogs(jobKey, page, size));
    }

    @PutMapping("/{jobKey}/enabled")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Enable or disable a job (runtime override)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminJobResponse> toggleJob(
            @PathVariable String jobKey,
            @Valid @RequestBody SuperAdminJobToggleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(jobAdminService.setEnabledOverride(
                jobKey,
                request.getEnabled(),
                principal != null ? principal.getAuthId() : null
        ));
    }
}
