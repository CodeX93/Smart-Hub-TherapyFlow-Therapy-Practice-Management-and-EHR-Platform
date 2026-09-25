package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingRequest;
import com.smart.therapy.flow.superadmin.dto.PlatformTenantRoutingResponse;
import com.smart.therapy.flow.superadmin.service.PlatformTenantRoutingService;
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

@RestController
@RequestMapping("/api/v1/super-admin/settings")
@RequiredArgsConstructor
@Tag(name = "Tenant Routing Settings", description = "Configure tenant routing behavior")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class PlatformTenantRoutingController {

    private final PlatformTenantRoutingService routingService;

    @GetMapping("/tenant-routing")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Get tenant routing settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformTenantRoutingResponse> getSettings() {
        PlatformTenantRoutingResponse response = routingService.getSettings();
        if (response == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(response);
    }

    @PutMapping("/tenant-routing")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update tenant routing settings", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<PlatformTenantRoutingResponse> upsert(
            @Valid @RequestBody PlatformTenantRoutingRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        PlatformTenantRoutingResponse response = routingService.upsert(request, principal != null ? principal.getAuthId() : null);
        return ResponseEntity.ok(response);
    }
}
