package com.smart.therapy.flow.audit.controller;

import com.smart.therapy.flow.audit.dto.AuditLogFilterRequest;
import com.smart.therapy.flow.audit.dto.AuditDashboardResponse;
import com.smart.therapy.flow.audit.dto.AuditLogResponse;
import com.smart.therapy.flow.audit.dto.AuditLogStatisticsResponse;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import com.smart.therapy.flow.common.security.CurrentUserService;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.security.StaffAuthorizationExpressions;
import com.smart.therapy.flow.common.service.GlobalActivityAuditService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * REST Controller for HIPAA Audit Trail
 * Provides endpoints for viewing audit logs, statistics, and exports
 */
@RestController
@RequestMapping("/api/v1/audit")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Audit Logs", description = "HIPAA Audit Trail Management")
@SecurityRequirement(name = "bearerAuth")
public class AuditLogController {

    private final AuditLogService auditLogService;
    private final CurrentUserService currentUserService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private final GlobalActivityAuditService globalActivityAuditService;

    /**
     * Get audit logs with filters
     * GET /api/v1/audit/logs
     */
    @GetMapping("/logs")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_READ)
    @Operation(summary = "Get audit logs", description = "Retrieve audit logs with optional filters. Admin and Supervisor only.")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Risk level filter (all, low, medium, high, critical)") 
            @RequestParam(required = false, defaultValue = "all") String riskLevel,
            
            @Parameter(description = "Filter PHI-relevant events only") 
            @RequestParam(required = false) Boolean hipaaOnly,
            
            @Parameter(description = "Action type filter (all or specific action)") 
            @RequestParam(required = false, defaultValue = "all") String action,
            
            @Parameter(description = "Username filter (partial match)") 
            @RequestParam(required = false) String username,
            
            @Parameter(description = "Client ID filter") 
            @RequestParam(required = false) Long clientId,
            
            @Parameter(description = "Resource type filter") 
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "Log level filter (all, trace, debug, info, warn, error)")
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            
            @Parameter(description = "Page number (0-indexed)") 
            @RequestParam(required = false, defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(required = false, defaultValue = "50") int size,
            
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        requireTenantAuditContext();
        
        // Log the export event if user is viewing audit logs
        if (userPrincipal != null) {
            Long userId = currentUserService.getCurrentUserId(userPrincipal);
            auditLogService.logDataExport(
                    userId,
                    userPrincipal.getLoginIdentifier(),
                    "audit_log_view",
                    null,
                    getClientIpAddress(request),
                    request.getHeader("User-Agent"),
                    null
            );
        }
        
        AuditLogFilterRequest filterRequest = buildFilterRequest(
                startDate,
                endDate,
                riskLevel,
                hipaaOnly,
                action,
                username,
                clientId,
                resourceType,
                logLevel
        );
        
        // Default to latest-first ordering (most recent audit events on top).
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "timestamp", "id"));
        Page<AuditLogResponse> logs = auditLogService.getAuditLogs(filterRequest, pageable);
        
        return ResponseEntity.ok(logs);
    }

    /**
     * Get HIPAA audit dashboard payload (cards, graph data, risk distribution, table)
     * GET /api/v1/audit/dashboard
     */
    @GetMapping("/dashboard")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_READ)
    @Operation(summary = "Get HIPAA audit dashboard", description = "Returns summary cards, user activity chart data, risk distribution, and paginated logs.")
    public ResponseEntity<AuditDashboardResponse> getAuditDashboard(
            @Parameter(description = "Start date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,

            @Parameter(description = "End date (YYYY-MM-DD)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,

            @Parameter(description = "Period preset (weekly, monthly, yearly). Used when date range is not fully provided.")
            @RequestParam(required = false, defaultValue = "weekly") String period,

            @Parameter(description = "Risk level filter (all, low, medium, high, critical)")
            @RequestParam(required = false, defaultValue = "all") String riskLevel,

            @Parameter(description = "Filter PHI-relevant events only")
            @RequestParam(required = false) Boolean hipaaOnly,

            @Parameter(description = "Action type filter (all or specific action)")
            @RequestParam(required = false, defaultValue = "all") String action,

            @Parameter(description = "Username filter (partial match)")
            @RequestParam(required = false) String username,

            @Parameter(description = "Client ID filter")
            @RequestParam(required = false) Long clientId,

            @Parameter(description = "Resource type filter")
            @RequestParam(required = false) String resourceType,

            @Parameter(description = "Log level filter (all, trace, debug, info, warn, error)")
            @RequestParam(required = false, defaultValue = "all") String logLevel,

            @Parameter(description = "Page number (0-indexed)")
            @RequestParam(required = false, defaultValue = "0") int page,

            @Parameter(description = "Page size")
            @RequestParam(required = false, defaultValue = "25") int size) {
        requireTenantAuditContext();

        AuditLogFilterRequest filterRequest = buildFilterRequest(
                startDate,
                endDate,
                riskLevel,
                hipaaOnly,
                action,
                username,
                clientId,
                resourceType,
                logLevel
        );

        Pageable pageable = PageRequest.of(page, size);
        AuditDashboardResponse dashboard = auditLogService.getAuditDashboard(filterRequest, period, pageable);
        return ResponseEntity.ok(dashboard);
    }

    /**
     * Get audit statistics
     * GET /api/v1/audit/stats
     */
    @GetMapping("/stats")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_READ)
    @Operation(summary = "Get audit statistics", description = "Retrieve audit log statistics. Admin and Supervisor only.")
    public ResponseEntity<AuditLogStatisticsResponse> getAuditStatistics(
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Risk level filter") 
            @RequestParam(required = false, defaultValue = "all") String riskLevel,
            
            @Parameter(description = "Filter PHI-relevant events only") 
            @RequestParam(required = false) Boolean hipaaOnly,

            @Parameter(description = "Log level filter (all, trace, debug, info, warn, error)")
            @RequestParam(required = false, defaultValue = "all") String logLevel) {
        requireTenantAuditContext();
        
        AuditLogFilterRequest filterRequest = buildFilterRequest(
                startDate,
                endDate,
                riskLevel,
                hipaaOnly,
                null,
                null,
                null,
                null,
                logLevel
        );
        
        AuditLogStatisticsResponse stats = auditLogService.getAuditStatistics(filterRequest);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Export audit logs as CSV
     * GET /api/v1/audit/export
     */
    @GetMapping("/export")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_EXPORT)
    @Operation(summary = "Export audit logs", description = "Export audit logs as CSV file. Admin and Supervisor only. Requires AUDIT_EXPORT plan feature.")
    public ResponseEntity<String> exportAuditLogs(
            @Parameter(description = "Start date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            
            @Parameter(description = "End date (YYYY-MM-DD)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            
            @Parameter(description = "Risk level filter") 
            @RequestParam(required = false, defaultValue = "all") String riskLevel,
            
            @Parameter(description = "Filter PHI-relevant events only") 
            @RequestParam(required = false) Boolean hipaaOnly,

            @Parameter(description = "Log level filter (all, trace, debug, info, warn, error)")
            @RequestParam(required = false, defaultValue = "all") String logLevel,
            
            @Parameter(description = "Export limit") 
            @RequestParam(required = false, defaultValue = "1000") int limit,
            
            @AuthenticationPrincipal AuthPrincipal userPrincipal,
            HttpServletRequest request) {
        requireTenantAuditContext();
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(orgId, SubscriptionFeatureService.FEATURE_AUDIT_EXPORT, null)) {
            throw new ForbiddenException("Audit export is not included in your plan. Please upgrade to export audit logs.");
        }
        if (orgId != null) {
            subscriptionFeatureService.consumeUsageOrThrow(orgId, SubscriptionFeatureService.FEATURE_AUDIT_EXPORT, 1L, "Audit export");
        }
        // Log the export event as CRITICAL risk
        if (userPrincipal != null) {
            Long userId = currentUserService.getCurrentUserId(userPrincipal);
            auditLogService.logDataExport(
                    userId,
                    userPrincipal.getLoginIdentifier(),
                    "audit_log_export",
                    null,
                    getClientIpAddress(request),
                    request.getHeader("User-Agent"),
                    null
            );
        }
        
        AuditLogFilterRequest filterRequest = buildFilterRequest(
                startDate,
                endDate,
                riskLevel,
                hipaaOnly,
                null,
                null,
                null,
                null,
                logLevel
        );
        
        String csv = auditLogService.exportAuditLogsAsCsv(filterRequest, limit);
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.setContentDispositionFormData("attachment", 
                "hipaa_audit_report_" + LocalDate.now() + ".csv");
        
        return new ResponseEntity<>(csv, headers, HttpStatus.OK);
    }

    /**
     * Get client-specific audit history
     * GET /api/v1/audit/clients/{clientId}
     */
    @GetMapping("/clients/{clientId}")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_READ_THERAPIST_OR_ADMIN)
    @Operation(summary = "Get client audit history", description = "Retrieve audit history for a specific client.")
    public ResponseEntity<List<AuditLogResponse>> getClientAuditHistory(
            @Parameter(description = "Client ID") 
            @PathVariable Long clientId,
            
            @Parameter(description = "Page number") 
            @RequestParam(required = false, defaultValue = "0") int page,
            
            @Parameter(description = "Page size") 
            @RequestParam(required = false, defaultValue = "50") int size) {
        requireTenantAuditContext();
        
        Pageable pageable = PageRequest.of(page, size);
        List<AuditLogResponse> logs = auditLogService.getClientAuditHistory(clientId, pageable);
        
        return ResponseEntity.ok(logs);
    }

    @GetMapping("/global-health")
    @PreAuthorize(StaffAuthorizationExpressions.AUDIT_READ)
    @Operation(summary = "Get global audit health (tenant)", description = "Shows counts for globally captured tenant audit events by module/action/errors.")
    public ResponseEntity<java.util.Map<String, Object>> getTenantGlobalAuditHealth(
            @Parameter(description = "Lookback window in hours")
            @RequestParam(required = false, defaultValue = "24") int hours,
            @Parameter(description = "Optional top-N limit applied to countsByResourceType and countsByAction")
            @RequestParam(required = false) Integer topN) {
        requireTenantAuditContext();
        return ResponseEntity.ok(globalActivityAuditService.getTenantGlobalAuditHealth(hours, topN));
    }

    /**
     * Helper method to extract client IP address from request
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private void requireTenantAuditContext() {
        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        if (orgId == null || schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
            throw new StoryApiException(HttpStatus.FORBIDDEN, "TENANT_CONTEXT_REQUIRED",
                    "HIPAA audit endpoints are tenant-scoped and require a resolved organisation context.");
        }
    }

    private static AuditLogFilterRequest buildFilterRequest(
            LocalDate startDate,
            LocalDate endDate,
            String riskLevel,
            Boolean hipaaOnly,
            String action,
            String username,
            Long clientId,
            String resourceType,
            String logLevel
    ) {
        AuditLogFilterRequest request = new AuditLogFilterRequest();
        request.setStartDate(startDate);
        request.setEndDate(endDate);
        request.setRiskLevel(riskLevel);
        request.setHipaaOnly(hipaaOnly);
        request.setAction(action);
        request.setUsername(username);
        request.setClientId(clientId);
        request.setResourceType(resourceType);
        request.setLogLevel(logLevel);
        return request;
    }
}
