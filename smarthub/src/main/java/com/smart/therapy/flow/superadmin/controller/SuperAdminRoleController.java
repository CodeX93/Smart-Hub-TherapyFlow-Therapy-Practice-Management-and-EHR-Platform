package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.auth.dto.RoleResponse;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.auth.service.TenantRbacSeedService;
import com.smart.therapy.flow.superadmin.dto.SuperAdminCreateRoleRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminRoleService;
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

import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/roles")
@RequiredArgsConstructor
@Tag(name = "Super Admin Roles", description = "Platform role management")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminRoleController {

    private final SuperAdminRoleService superAdminRoleService;
    private final TenantRbacSeedService tenantRbacSeedService;

    @PostMapping
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Create role with permissions", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> createRole(
            @Valid @RequestBody SuperAdminCreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            RoleResponse response = superAdminRoleService.createRole(request, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.status(201).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List roles", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> listRoles(@RequestParam(required = false) Long organisationId) {
        try {
            return ResponseEntity.ok(superAdminRoleService.listRoles(organisationId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{roleId}")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get role", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> getRole(@PathVariable Long roleId) {
        try {
            return ResponseEntity.ok(superAdminRoleService.getRole(roleId));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{roleId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update role with permissions", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateRole(
            @PathVariable Long roleId,
            @Valid @RequestBody SuperAdminCreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            RoleResponse response = superAdminRoleService.updateRole(roleId, request, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{roleId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Delete role", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> deleteRole(
            @PathVariable Long roleId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            superAdminRoleService.deleteRole(roleId, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/reseed/{organisationId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Reseed tenant RBAC defaults for one organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> reseedOrganisationRoles(@PathVariable Long organisationId) {
        tenantRbacSeedService.seedDefaultsForOrganisation(organisationId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "organisationId", organisationId,
                "message", "Tenant RBAC defaults reseeded"
        ));
    }

    @PostMapping("/reseed-all")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Reseed tenant RBAC defaults for all organisations", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> reseedAllOrganisationRoles() {
        int processed = tenantRbacSeedService.seedDefaultsForAllOrganisations();
        return ResponseEntity.ok(Map.of(
                "success", true,
                "processedOrganisations", processed,
                "message", "Tenant RBAC defaults reseeded for all organisations"
        ));
    }
}
