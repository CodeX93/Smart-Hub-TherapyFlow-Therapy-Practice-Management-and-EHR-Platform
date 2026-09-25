package com.smart.therapy.flow.auth.controller;

import com.smart.therapy.flow.auth.dto.*;
import com.smart.therapy.flow.auth.service.RolePermissionService;
import com.smart.therapy.flow.common.dto.PaginatedResponse;
import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.PermissionConstants;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.subscription.service.SubscriptionFeatureService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;
    private final SubscriptionFeatureService subscriptionFeatureService;
    private static final String TENANT_OR_PLATFORM_RBAC_MANAGE =
            PermissionConstants.USER_MANAGE + " or " + RoleConstants.ROLE_PLATFORM_SUPER_ADMIN + " or " + PermissionConstants.PLATFORM_MANAGE;

    /** Require ROLES_PERMISSIONS plan feature for this organisation. */
    private void requireRolesPermissionsFeature() {
        Long orgId = TenantContext.getOrganisationId();
        if (orgId != null && !subscriptionFeatureService.isFeatureEnabled(orgId, SubscriptionFeatureService.FEATURE_ROLES_PERMISSIONS, null)) {
            throw new ForbiddenException("Roles and permissions are not included in your plan. Please upgrade to Professional or higher.");
        }
    }

    private void requireTenantContext(String message) {
        if (TenantContext.getOrganisationId() == null) {
            throw new ForbiddenException(message);
        }
    }

    // ========== ROLE ENDPOINTS ==========

    @GetMapping("/roles")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<List<RoleResponse>> getRoles(
            @RequestParam(required = false) String search
    ) {
        requireRolesPermissionsFeature();
        List<RoleResponse> roles = rolePermissionService.getRoles(search);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/tenant-admin/roles")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: list organisation roles")
    public ResponseEntity<List<RoleResponse>> getOrganisationRolesForTenantAdmin(
            @RequestParam(required = false) String search
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        List<RoleResponse> roles = rolePermissionService.getRoles(search);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/roles/paged")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<PaginatedResponse<RoleResponse>> getRolesPaged(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "displayName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        requireRolesPermissionsFeature();
        PaginatedResponse<RoleResponse> roles = rolePermissionService.getRolesPaged(search, page, pageSize, sortBy, sortDirection);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/tenant-admin/roles/paged")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: list organisation roles (paged)")
    public ResponseEntity<PaginatedResponse<RoleResponse>> getOrganisationRolesPagedForTenantAdmin(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize,
            @RequestParam(defaultValue = "displayName") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDirection
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        PaginatedResponse<RoleResponse> roles = rolePermissionService
                .getRolesPaged(search, page, pageSize, sortBy, sortDirection);
        return ResponseEntity.ok(roles);
    }

    @GetMapping("/tenant-admin/roles/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: get organisation role by id")
    public ResponseEntity<RoleResponse> getRoleForTenantAdmin(@PathVariable("id") Long id) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.getRole(id));
    }

    @GetMapping("/roles/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<RoleResponse> getRole(@PathVariable("id") Long id) {
        requireRolesPermissionsFeature();
        RoleResponse role = rolePermissionService.getRole(id);
        return ResponseEntity.ok(role);
    }

    @PostMapping("/roles")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(
            summary = "Create new role",
            description = """
                    Create a new role in the system.
                    
                    **Required Fields:**
                    - `name` (REQUIRED): Role name/identifier (must be unique)
                    - `displayName` (REQUIRED): Human-readable display name
                    
                    **Optional Fields:**
                    - `description` (optional): Role description
                    - `isSystem` (optional, default: false): Whether this is a system role
                    - `isActive` (optional, default: true): Whether the role is active
                    - `permissions` (optional): List of permission IDs to assign to this role
                    
                    **Requires:** USER_MANAGE permission.
                    """,
            security = @io.swagger.v3.oas.annotations.security.SecurityRequirement(name = "BearerAuth"),
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Role information to create",
                    required = true,
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateRoleRequest.class),
                            examples = @io.swagger.v3.oas.annotations.media.ExampleObject(
                                    name = "Create Role",
                                    value = """
                                            {
                                              "name": "CUSTOM_ROLE",
                                              "displayName": "Custom Role",
                                              "description": "A custom role with specific permissions",
                                              "isSystem": false,
                                              "isActive": true,
                                              "permissions": [1, 2, 3]
                                            }
                                            """
                            )
                    )
            )
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "Role created successfully",
                    content = @io.swagger.v3.oas.annotations.media.Content(
                            mediaType = "application/json",
                            schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = RoleResponse.class)
                    )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation error or role name already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Insufficient permissions - USER_MANAGE permission required"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Not authenticated")
    })
    public ResponseEntity<RoleResponse> createRole(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        RoleResponse role = rolePermissionService.createRole(request, principal);
        return ResponseEntity.status(201).body(role);
    }

    @PostMapping("/tenant-admin/roles")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: create organisation role")
    public ResponseEntity<RoleResponse> createRoleForTenantAdmin(
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        return ResponseEntity.status(201).body(rolePermissionService.createRole(request, principal));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<RoleResponse> updateRole(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        RoleResponse role = rolePermissionService.updateRole(id, request, principal);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/tenant-admin/roles/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: update organisation role")
    public ResponseEntity<RoleResponse> updateRoleForTenantAdmin(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateRoleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.updateRole(id, request, principal));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<Void> deleteRole(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        rolePermissionService.deleteRole(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tenant-admin/roles/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: delete organisation role")
    public ResponseEntity<Void> deleteRoleForTenantAdmin(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        rolePermissionService.deleteRole(id, principal);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<RoleResponse> updateRolePermissions(
            @PathVariable("id") Long roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        RoleResponse role = rolePermissionService.updateRolePermissions(roleId, request.getPermissionIds(), principal);
        return ResponseEntity.ok(role);
    }

    @PutMapping("/tenant-admin/roles/{id}/permissions")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: update organisation role permissions")
    public ResponseEntity<RoleResponse> updateRolePermissionsForTenantAdmin(
            @PathVariable("id") Long roleId,
            @Valid @RequestBody UpdateRolePermissionsRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin role endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.updateRolePermissions(roleId, request.getPermissionIds(), principal));
    }

    // ========== PERMISSION ENDPOINTS ==========

    @GetMapping("/permissions")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<List<PermissionResponse>> getPermissions() {
        requireRolesPermissionsFeature();
        List<PermissionResponse> permissions = rolePermissionService.getPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/tenant-admin/permissions")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: list organisation permissions")
    public ResponseEntity<List<PermissionResponse>> getOrganisationPermissionsForTenantAdmin() {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        List<PermissionResponse> permissions = rolePermissionService.getPermissions();
        return ResponseEntity.ok(permissions);
    }

    @GetMapping("/tenant-admin/permissions/policy")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: get permission policy")
    public ResponseEntity<TenantPermissionPolicyResponse> getTenantPermissionPolicy() {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.getTenantPermissionPolicy());
    }

    @GetMapping("/tenant-admin/permissions/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: get organisation permission by id")
    public ResponseEntity<PermissionResponse> getPermissionForTenantAdmin(@PathVariable("id") Long id) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.getPermission(id));
    }

    @GetMapping("/permissions/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<PermissionResponse> getPermission(@PathVariable("id") Long id) {
        requireRolesPermissionsFeature();
        PermissionResponse permission = rolePermissionService.getPermission(id);
        return ResponseEntity.ok(permission);
    }

    @PostMapping("/permissions")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<PermissionResponse> createPermission(
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        PermissionResponse permission = rolePermissionService.createPermission(request, principal);
        return ResponseEntity.status(201).body(permission);
    }

    @PostMapping("/tenant-admin/permissions")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: create organisation permission")
    public ResponseEntity<PermissionResponse> createPermissionForTenantAdmin(
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        return ResponseEntity.status(201).body(rolePermissionService.createPermission(request, principal));
    }

    @PutMapping("/permissions/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<PermissionResponse> updatePermission(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        PermissionResponse permission = rolePermissionService.updatePermission(id, request, principal);
        return ResponseEntity.ok(permission);
    }

    @PutMapping("/tenant-admin/permissions/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: update organisation permission")
    public ResponseEntity<PermissionResponse> updatePermissionForTenantAdmin(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreatePermissionRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        return ResponseEntity.ok(rolePermissionService.updatePermission(id, request, principal));
    }

    @DeleteMapping("/permissions/{id}")
    @PreAuthorize(TENANT_OR_PLATFORM_RBAC_MANAGE)
    public ResponseEntity<Void> deletePermission(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        rolePermissionService.deletePermission(id, principal);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/tenant-admin/permissions/{id}")
    @PreAuthorize(PermissionConstants.USER_MANAGE)
    @io.swagger.v3.oas.annotations.Operation(summary = "Tenant admin: delete organisation permission")
    public ResponseEntity<Void> deletePermissionForTenantAdmin(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        requireRolesPermissionsFeature();
        requireTenantContext("Tenant admin permission endpoint requires organisation context");
        rolePermissionService.deletePermission(id, principal);
        return ResponseEntity.noContent().build();
    }
}
