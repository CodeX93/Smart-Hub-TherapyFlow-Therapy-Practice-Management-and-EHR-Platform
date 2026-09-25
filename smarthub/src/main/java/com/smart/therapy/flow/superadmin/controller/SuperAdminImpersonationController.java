package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.auth.dto.JwtAuthenticationResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminExchangeImpersonationRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminImpersonationSessionResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminImpersonationPolicyResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminStartImpersonationRequest;
import com.smart.therapy.flow.superadmin.dto.SuperAdminUpsertImpersonationPolicyRequest;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationPolicy;
import com.smart.therapy.flow.superadmin.entity.PlatformImpersonationSession;
import com.smart.therapy.flow.superadmin.service.SuperAdminImpersonationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/impersonation")
@RequiredArgsConstructor
@Tag(name = "Super Admin Impersonation", description = "Impersonation policy and sessions")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminImpersonationController {

    private final SuperAdminImpersonationService impersonationService;

    @GetMapping("/policy")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get impersonation policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminImpersonationPolicyResponse> policy() {
        return ResponseEntity.ok(toPolicyResponse(impersonationService.getPolicy()));
    }

    @PutMapping("/policy")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update impersonation policy", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<SuperAdminImpersonationPolicyResponse> upsertPolicy(
            @Valid @RequestBody SuperAdminUpsertImpersonationPolicyRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(toPolicyResponse(impersonationService.upsertPolicy(
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
        )));
    }

    @PostMapping("/start")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Start impersonation session", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> start(
            @Valid @RequestBody SuperAdminStartImpersonationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            SuperAdminImpersonationService.CreatedImpersonation created = impersonationService.startSession(
                    principal != null ? principal.getAuthId() : null,
                    request.getTargetAuthId(),
                    request.getOrganisationId(),
                    request.getReason(),
                    request.getDurationMinutes()
            );
            PlatformImpersonationSession s = created.session();
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "sessionId", s.getId(),
                    "organisationId", s.getOrganisation().getId(),
                    "targetAuthId", s.getTargetAuthId(),
                    "status", s.getStatus(),
                    "expiresAt", s.getExpiresAt(),
                    "impersonationToken", created.rawToken()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/exchange")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Exchange impersonation token for JWT", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> exchange(
            @Valid @RequestBody SuperAdminExchangeImpersonationRequest request,
            @AuthenticationPrincipal AuthPrincipal principal,
            HttpServletRequest httpRequest
    ) {
        try {
            SuperAdminImpersonationService.ExchangedImpersonation exchanged = impersonationService.exchangeSessionToken(
                    principal != null ? principal.getAuthId() : null,
                    request.getImpersonationToken(),
                    resolveIpAddress(httpRequest)
            );
            PlatformImpersonationSession s = exchanged.session();
            JwtAuthenticationResponse response = JwtAuthenticationResponse.builder()
                    .accessToken(exchanged.accessToken())
                    .refreshToken(null)
                    .tokenType("Bearer")
                    .userId(null)
                    .username(exchanged.loginIdentifier())
                    .email(exchanged.loginIdentifier())
                    .roles(exchanged.roles())
                    .permissions(exchanged.permissions())
                    .expiresIn(java.time.Duration.between(java.time.Instant.now(), s.getExpiresAt()).toMillis())
                    .passwordChangeRequired(false)
                    .message("Impersonation JWT issued")
                    .build();
            return ResponseEntity.ok(Map.of(
                    "sessionId", s.getId(),
                    "organisationId", s.getOrganisation().getId(),
                    "targetAuthId", s.getTargetAuthId(),
                    "status", s.getStatus(),
                    "expiresAt", s.getExpiresAt(),
                    "auth", response
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/sessions")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List impersonation sessions", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<List<SuperAdminImpersonationSessionResponse>> sessions() {
        List<SuperAdminImpersonationSessionResponse> body = impersonationService.listSessions().stream()
                .map(this::toResponse)
                .toList();
        return ResponseEntity.ok(body);
    }

    @PostMapping("/sessions/{id}/end")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "End impersonation session", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> end(@PathVariable Long id, @AuthenticationPrincipal AuthPrincipal principal) {
        try {
            PlatformImpersonationSession session = impersonationService.endSession(id, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.ok(Map.of("sessionId", session.getId(), "status", session.getStatus(), "endedAt", session.getEndedAt()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("error", e.getMessage()));
        }
    }

    private SuperAdminImpersonationSessionResponse toResponse(PlatformImpersonationSession row) {
        SuperAdminImpersonationSessionResponse response = new SuperAdminImpersonationSessionResponse();
        response.setSessionId(row.getId());
        response.setSuperAdminAuthId(row.getSuperAdminAuthId());
        response.setTargetAuthId(row.getTargetAuthId());
        response.setOrganisationId(row.getOrganisation().getId());
        response.setStatus(row.getStatus());
        response.setStartedAt(row.getStartedAt());
        response.setExpiresAt(row.getExpiresAt());
        response.setEndedAt(row.getEndedAt());
        response.setReason(row.getReason());
        return response;
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
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .toList();
    }

    private List<Long> parseLongCsv(String csv) {
        if (csv == null || csv.isBlank()) {
            return List.of();
        }
        return java.util.Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(Long::valueOf)
                .toList();
    }

    private String resolveIpAddress(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
