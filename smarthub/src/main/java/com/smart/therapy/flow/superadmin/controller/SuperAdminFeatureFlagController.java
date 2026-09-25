package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.FeatureFlagDisableAllRequest;
import com.smart.therapy.flow.superadmin.dto.FeatureFlagToggleRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminFeatureFlagService;
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
@RequestMapping("/api/v1/super-admin/features")
@RequiredArgsConstructor
@Tag(name = "Super Admin Feature Flags", description = "Global feature flag toggles")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminFeatureFlagController {

    private final SuperAdminFeatureFlagService superAdminFeatureFlagService;

    @PatchMapping("/flags/{key}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Toggle feature flag", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> toggleFeature(
            @PathVariable String key,
            @Valid @RequestBody FeatureFlagToggleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminFeatureFlagService.toggleFeature(
                key,
                Boolean.TRUE.equals(request.getEnabled()),
                principal != null ? principal.getAuthId() : null
        ));
    }

    @GetMapping("/flags/{key}/impact")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Preview impact of feature toggle", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> previewToggleImpact(@PathVariable String key) {
        return ResponseEntity.ok(superAdminFeatureFlagService.previewToggle(key));
    }

    @PatchMapping("/disable-all")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Disable all features globally", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> disableAll(
            @Valid @RequestBody FeatureFlagDisableAllRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        return ResponseEntity.ok(superAdminFeatureFlagService.disableAllFeatures(
                request.getConfirmation(),
                principal != null ? principal.getAuthId() : null
        ));
    }

    @GetMapping("/disable-all/impact")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Preview impact of disabling all features", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> previewDisableAllImpact() {
        return ResponseEntity.ok(superAdminFeatureFlagService.previewDisableAll());
    }
}
