package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.service.GlobalActivityAuditService;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminCreateIncidentRequest;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpdateIncidentRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformIncident;
import com.smart.therapy.flow.superadmin.service.SuperAdminOrganisationQueryService;
import com.smart.therapy.flow.superadmin.service.SuperAdminSystemService;
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

import java.lang.management.ManagementFactory;
import java.util.List;
import java.util.Map;
import java.time.Instant;

@RestController
@RequestMapping("/api/v1/super-admin/system")
@RequiredArgsConstructor
@Tag(name = "Super Admin System", description = "Platform health and incidents")
@PreAuthorize(RoleConstants.PLATFORM_READ)
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminSystemController {

    private final SuperAdminSystemService systemService;
    private final PlatformAuditService platformAuditService;
    private final SuperAdminOrganisationQueryService organisationQueryService;
    private final GlobalActivityAuditService globalActivityAuditService;

    @GetMapping("/health")
    @Operation(summary = "System health", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(systemService.getSystemHealth());
    }

    @GetMapping("/uptime")
    @Operation(summary = "System uptime", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> uptime() {
        long uptimeMs = ManagementFactory.getRuntimeMXBean().getUptime();
        return ResponseEntity.ok(Map.of("uptimeMs", uptimeMs));
    }

    @GetMapping("/incidents")
    @Operation(summary = "List incidents", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<PlatformIncident>> incidents(@RequestParam(required = false) String status) {
        return ResponseEntity.ok(systemService.listIncidents(status));
    }

    @PostMapping("/incidents")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create incident", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformIncident> createIncident(
            @Valid @RequestBody SuperAdminCreateIncidentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformIncident saved = systemService.createIncident(
                request.getTitle(),
                request.getDescription(),
                request.getServiceName(),
                request.getSeverity()
        );
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "SYSTEM_INCIDENT_CREATED",
                "PlatformIncident",
                String.valueOf(saved.getId()),
                "severity=" + saved.getSeverity()
        );
        return ResponseEntity.ok(saved);
    }

    @PatchMapping("/incidents/{id}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update incident", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformIncident> updateIncident(
            @PathVariable Long id,
            @Valid @RequestBody SuperAdminUpdateIncidentRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformIncident saved = systemService.updateIncident(
                id,
                request.getStatus(),
                request.getSeverity(),
                request.getDescription()
        );
        platformAuditService.log(
                principal != null ? principal.getAuthId() : null,
                "SYSTEM_INCIDENT_UPDATED",
                "PlatformIncident",
                String.valueOf(saved.getId()),
                "status=" + saved.getStatus()
        );
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "Platform audit logs", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<AuditLogEntryResponse>> auditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) Long authId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String resourceType,
            @RequestParam(required = false) String resourceId,
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Instant createdFrom,
            @RequestParam(required = false) Instant createdTo,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String order
    ) {
        return ResponseEntity.ok(organisationQueryService.listAuditLogResponses(
                page,
                size,
                authId,
                action,
                resourceType,
                resourceId,
                logLevel,
                q,
                createdFrom,
                createdTo,
                sort,
                order
        ));
    }

    @GetMapping("/global-audit-health")
    @Operation(summary = "Global platform audit health", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Map<String, Object>> globalAuditHealth(
            @RequestParam(required = false, defaultValue = "24") int hours,
            @RequestParam(required = false) Integer topN
    ) {
        return ResponseEntity.ok(globalActivityAuditService.getPlatformGlobalAuditHealth(hours, topN));
    }

}
