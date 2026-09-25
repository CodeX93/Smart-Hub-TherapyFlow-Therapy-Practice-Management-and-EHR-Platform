package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUsageTargetsResponse;
import com.smart.therapy.flow.superadmin.service.SuperAdminUsageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/super-admin")
@RequiredArgsConstructor
@Tag(name = "Super Admin Usage", description = "Metered feature usage counters")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminUsageController {

    private final SuperAdminUsageService superAdminUsageService;

    @GetMapping("/usage")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get metered usage for organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminUsageResponse> getUsage(
            @RequestParam("orgId") Long organisationId,
            @RequestParam(value = "period", required = false) String period,
            @RequestParam(value = "targetKey", required = false) String targetKey
    ) {
        return ResponseEntity.ok(superAdminUsageService.getUsage(organisationId, period, targetKey));
    }

    @GetMapping("/usage/targets")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get metered usage for organisation targets", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminUsageTargetsResponse> getUsageTargets(
            @RequestParam("orgId") Long organisationId,
            @RequestParam(value = "period", required = false) String period
    ) {
        return ResponseEntity.ok(superAdminUsageService.getUsageTargets(organisationId, period));
    }

    @GetMapping(value = "/usage/targets/export", produces = "text/csv")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Export metered usage for organisation targets as CSV", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<String> exportUsageTargetsCsv(
            @RequestParam("orgId") Long organisationId,
            @RequestParam(value = "period", required = false) String period
    ) {
        String csv = superAdminUsageService.getUsageTargetsCsv(organisationId, period);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=usage-targets.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv);
    }
}
