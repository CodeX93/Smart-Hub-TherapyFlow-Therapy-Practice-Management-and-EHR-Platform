package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.BreakGlassService;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.BreakGlassAccessRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminImpersonationPolicyResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpsertImpersonationPolicyRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationPolicy;
import com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/v1/super-admin/security")
@RequiredArgsConstructor
@Tag(name = "Super Admin Security", description = "Global security configuration")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminSecurityController {

    private final SuperAdminImpersonationService impersonationService;
    private final BreakGlassService breakGlassService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get global security settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminImpersonationPolicyResponse> getSecuritySettings() {
        return ResponseEntity.ok(toPolicyResponse(impersonationService.getPolicy()));
    }

    @PutMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update global security settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminImpersonationPolicyResponse> updateSecuritySettings(
            @Valid @RequestBody SuperAdminUpsertImpersonationPolicyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformImpersonationPolicy updated = impersonationService.upsertPolicy(
                request.getEnabled(),
                request.getRequireReason(),
                request.getMinReasonLength(),
                request.getMaxDurationMinutes(),
                request.getAllowCrossOrganisation(),
                request.getAllowedRoles(),
                request.getDeniedRoles(),
                request.getAllowedOrgIds(),
                request.getDeniedOrgIds(),
                principal != null ? principal.getAuthId() : null
        );
        return ResponseEntity.ok(toPolicyResponse(updated));
    }

    @PostMapping("/break-glass")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Request break-glass access (stub)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<BreakGlassService.BreakGlassResult> requestBreakGlassAccess(
            @Valid @RequestBody BreakGlassAccessRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        BreakGlassService.BreakGlassResult result = breakGlassService.requestAccess(
                principal != null ? principal.getAuthId() : null,
                request.getReason(),
                request.getResourceType(),
                request.getResourceId());
        return ResponseEntity.ok(result);
    }

    private SuperAdminImpersonationPolicyResponse toPolicyResponse(PlatformImpersonationPolicy policy) {
        SuperAdminImpersonationPolicyResponse response = new SuperAdminImpersonationPolicyResponse();
        response.setId(policy.getId());
        response.setEnabled(policy.getEnabled());
        response.setRequireReason(policy.getRequireReason());
        response.setMinReasonLength(policy.getMinReasonLength());
        response.setMaxDurationMinutes(policy.getMaxDurationMinutes());
        response.setAllowCrossOrganisation(policy.getAllowCrossOrganisation());
        response.setAllowedRoles(parseRoleCsv(policy.getAllowedRoleNames()));
        response.setDeniedRoles(parseRoleCsv(policy.getDeniedRoleNames()));
        response.setAllowedOrgIds(parseLongCsv(policy.getAllowedOrgIds()));
        response.setDeniedOrgIds(parseLongCsv(policy.getDeniedOrgIds()));
        response.setUpdatedByAuthId(policy.getUpdatedByAuthId());
        response.setCreatedAt(policy.getCreatedAt());
        response.setUpdatedAt(policy.getUpdatedAt());
        return response;
    }

    private List<String> parseRoleCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .toList();
    }
}
