package com.smart.therapy.flow.superadmin.controller;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.security.RoleConstants;
import com.smart.therapy.flow.superadmin.dto.SuperAdminDisableUserRequest;
import com.smart.therapy.flow.superadmin.dto.RolesPermissionsMatrixUpdateRequest;
import com.smart.therapy.flow.superadmin.dto.RolePermissionToggleRequest;
import com.smart.therapy.flow.superadmin.service.SuperAdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/super-admin/users")
@RequiredArgsConstructor
@Tag(name = "Super Admin Users", description = "Global user management in platform scope")
@Validated
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Validation error"),
        @ApiResponse(responseCode = "403", description = "Forbidden"),
        @ApiResponse(responseCode = "404", description = "Not found")
})
public class SuperAdminUserController {

    private final SuperAdminUserService superAdminUserService;

    @GetMapping
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "List users globally", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> list(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String identityType,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long organisationId,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                    java.time.Instant lastLoginFrom,
            @RequestParam(required = false) @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE_TIME)
                    java.time.Instant lastLoginTo,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "50") @Min(1) @Max(100) int size
    ) {
        try {
            var result = superAdminUserService.listUsers(
                    search,
                    role,
                    identityType,
                    status,
                    organisationId,
                    active,
                    lastLoginFrom,
                    lastLoginTo,
                    page,
                    size
            );
            List<Map<String, Object>> items = result.getContent().stream()
                    .map(superAdminUserService::buildUserDetails)
                    .toList();
            return ResponseEntity.ok(Map.of(
                    "items", items,
                    "page", result.getNumber(),
                    "size", result.getSize(),
                    "total", result.getTotalElements()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{authId}/disable")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Disable user identity", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> disable(
            @PathVariable Long authId,
            @Valid @RequestBody SuperAdminDisableUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            AuthIdentity saved = superAdminUserService.disableUser(
                    authId,
                    principal != null ? principal.getAuthId() : null,
                    request.getReason()
            );
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{authId}/enable")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Enable user identity", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> enable(
            @PathVariable Long authId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            AuthIdentity saved = superAdminUserService.enableUser(authId, principal != null ? principal.getAuthId() : null);
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{authId}/organisations/{organisationId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Link user to organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> linkOrganisation(
            @PathVariable Long authId,
            @PathVariable Long organisationId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            superAdminUserService.linkUserToOrganisation(authId, organisationId, principal != null ? principal.getAuthId() : null);
            AuthIdentity saved = superAdminUserService.getIdentityOrThrow(authId);
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{authId}/organisations/{organisationId}")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Unlink user from organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> unlinkOrganisation(
            @PathVariable Long authId,
            @PathVariable Long organisationId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            superAdminUserService.unlinkUserFromOrganisation(authId, organisationId, principal != null ? principal.getAuthId() : null);
            AuthIdentity saved = superAdminUserService.getIdentityOrThrow(authId);
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{authId}/organisations/{organisationId}/disable")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Disable user for organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> disableForOrganisation(
            @PathVariable Long authId,
            @PathVariable Long organisationId,
            @Valid @RequestBody SuperAdminDisableUserRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            AuthIdentity saved = superAdminUserService.disableUserForOrganisation(
                    authId,
                    organisationId,
                    principal != null ? principal.getAuthId() : null,
                    request.getReason()
            );
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{authId}/organisations/{organisationId}/enable")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Enable user for organisation", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> enableForOrganisation(
            @PathVariable Long authId,
            @PathVariable Long organisationId,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            AuthIdentity saved = superAdminUserService.enableUserForOrganisation(
                    authId,
                    organisationId,
                    principal != null ? principal.getAuthId() : null
            );
            return ResponseEntity.ok(superAdminUserService.buildUserDetails(saved));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{authId}/roles-permissions")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get roles and permissions for user", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> getUserRolesPermissions(
            @PathVariable Long authId,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String permissionGroup
    ) {
        try {
            return ResponseEntity.ok(superAdminUserService.getUserRolesPermissions(authId, module, permissionGroup));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/roles-permissions-matrix")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Get roles-permissions matrix", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> getRolesPermissionsMatrix(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String permissionGroup
    ) {
        try {
            return ResponseEntity.ok(superAdminUserService.getRolesPermissionsMatrix(module, permissionGroup));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/roles-permissions-matrix")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Update roles-permissions matrix", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> updateRolesPermissionsMatrix(
            @Valid @RequestBody RolesPermissionsMatrixUpdateRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(
                    superAdminUserService.updateRolesPermissionsMatrix(
                            request.getUpdates(),
                            principal != null ? principal.getAuthId() : null
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PatchMapping("/roles-permissions-matrix/toggle")
    @PreAuthorize(RoleConstants.ROLE_PLATFORM_SUPER_ADMIN)
    @Operation(summary = "Toggle a single role-permission matrix cell", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> toggleRolePermissionCell(
            @Valid @RequestBody RolePermissionToggleRequest request,
            @AuthenticationPrincipal AuthPrincipal principal
    ) {
        try {
            return ResponseEntity.ok(
                    superAdminUserService.updateRolePermissionCell(
                            request.getRoleName(),
                            request.getPermissionName(),
                            request.getGranted(),
                            principal != null ? principal.getAuthId() : null
                    )
            );
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping(value = "/roles-permissions-matrix/export", produces = "text/csv")
    @PreAuthorize(RoleConstants.PLATFORM_READ)
    @Operation(summary = "Export roles-permissions matrix (CSV)", security = @SecurityRequirement(name = "BearerAuth"))
    public ResponseEntity<Object> exportRolesPermissionsMatrix(
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String permissionGroup
    ) {
        try {
            String csv = superAdminUserService.exportRolesPermissionsMatrixCsv(module, permissionGroup);
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=roles_permissions_matrix.csv")
                    .contentType(org.springframework.http.MediaType.valueOf("text/csv"))
                    .body(csv);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
