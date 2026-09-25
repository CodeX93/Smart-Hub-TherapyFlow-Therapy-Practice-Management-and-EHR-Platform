package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.exception.ForbiddenException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * EXAMPLES: Dynamic Permission System Usage
 * 
 * This file demonstrates how to use the dynamic permission system.
 * DO NOT use this class in production - it's for documentation only.
 * 
 * Key Concepts:
 * 1. Permissions are stored in database and loaded dynamically
 * 2. No code changes needed when admins add new permissions
 * 3. Use permission names as strings (not hardcoded constants)
 * 4. Combine permission checks with data scope enforcement
 */
@RestController
@RequestMapping("/api/v1/examples/permissions")
@RequiredArgsConstructor
public class DynamicPermissionExamples {

    private final PermissionChecker permissionChecker;

    // ============================================
    // EXAMPLE 1: Controller with Dynamic @PreAuthorize
    // ============================================

    /**
     * Example: Using dynamic permission string directly in @PreAuthorize
     * 
     * When admin adds "CUSTOM_FEATURE_ACCESS" permission to database,
     * this endpoint automatically works - no code changes needed!
     */
    @GetMapping("/custom-feature")
    @PreAuthorize("hasAuthority('CUSTOM_FEATURE_ACCESS')")
    public String customFeature() {
        return "Access granted to custom feature";
    }

    /**
     * Example: Using permission from method parameter
     * This allows even more flexibility - permission name comes from request
     */
    @GetMapping("/dynamic/{permissionName}")
    @PreAuthorize("hasAuthority(#permissionName)")
    public String dynamicPermission(@PathVariable String permissionName) {
        return "Access granted with permission: " + permissionName;
    }

    /**
     * Example: Multiple permissions (OR logic)
     */
    @GetMapping("/client-data")
    @PreAuthorize("hasAuthority('CLIENT_VIEW_OWN') or hasAuthority('CLIENT_VIEW_TEAM') or hasAuthority('CLIENT_VIEW_ALL')")
    public String getClientData() {
        return "Client data";
    }

    /**
     * Example: Multiple permissions (AND logic)
     */
    @PostMapping("/sensitive-operation")
    @PreAuthorize("hasAuthority('SESSION_CREATE') and hasAuthority('CLIENT_EDIT')")
    public String sensitiveOperation() {
        return "Sensitive operation completed";
    }

    // ============================================
    // EXAMPLE 2: Service Layer Permission Checking
    // ============================================

    /**
     * Example service method showing permission + data scope enforcement
     * 
     * This is the CORRECT pattern for clinical systems:
     * 1. Check permission (what can user do?)
     * 2. Check data scope (which data can user access?)
     */
    public String getClientWithScope(
            Long clientId,
            @AuthenticationPrincipal AuthPrincipal requester
    ) {
        // Step 1: Check if user has ANY client view permission
        boolean canViewAll = permissionChecker.hasPermission(requester, "CLIENT_VIEW_ALL");
        boolean canViewTeam = permissionChecker.hasPermission(requester, "CLIENT_VIEW_TEAM");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "CLIENT_VIEW_OWN");

        if (!canViewAll && !canViewTeam && !canViewOwn) {
            throw new ForbiddenException("No permission to view clients");
        }

        // Step 2: Load the client (pseudo-code - replace with actual repository call)
        // Client client = clientRepository.findById(clientId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Client not found"));

        // Step 3: Enforce data scope based on permission
        if (canViewAll) {
            // Admin can view all - no scope restriction
            // return client;
        } else if (canViewTeam) {
            // Supervisor can view team's clients
            // if (!isClientInSupervisorTeam(client, requester.getId())) {
            //     throw new ForbiddenException("Client not in your team");
            // }
            // return client;
        } else if (canViewOwn) {
            // Therapist can view only assigned clients
            // if (!Objects.equals(client.getAssignedTherapist().getId(), requester.getId())) {
            //     throw new ForbiddenException("Client not assigned to you");
            // }
            // return client;
        }

        return "Client data";
    }

    // ============================================
    // EXAMPLE 3: Using AuthPrincipal with PermissionChecker
    // ============================================

    /**
     * Example: Using PermissionChecker with AuthPrincipal (unified auth)
     */
    public void exampleWithAuthPrincipalHelper(AuthPrincipal requester) {
        // Direct check
        if (permissionChecker.hasPermission(requester, "SESSION_CREATE")) {
            // Create session
        }

        // Check multiple (OR)
        if (permissionChecker.hasAnyPermission(requester, "CLIENT_VIEW_ALL", "CLIENT_VIEW_TEAM")) {
            // Can view clients
        }

        // Check multiple (AND)
        if (permissionChecker.hasAllPermissions(requester, "BILLING_VIEW", "BILLING_MANAGE")) {
            // Can view and manage billing
        }
    }

    // ============================================
    // EXAMPLE 4: PermissionChecker Service Methods
    // ============================================

    /**
     * Example: Using PermissionChecker service
     */
    public void exampleWithPermissionChecker(AuthPrincipal requester) {
        // Simple check
        if (permissionChecker.hasPermission(requester, "ASSESSMENT_ASSIGN")) {
            // Assign assessment
        }

        // Require permission (throws exception if not present)
        permissionChecker.requirePermission(requester, "FORM_TEMPLATE_MANAGE");

        // Check any
        if (permissionChecker.hasAnyPermission(requester, "ROOM_MANAGE", "ROOM_VIEW")) {
            // Can manage or view rooms
        }

        // Check all
        if (permissionChecker.hasAllPermissions(requester, "USER_MANAGE", "AUDIT_VIEW")) {
            // Can manage users and view audit
        }

        // Get all user permissions
        var permissions = permissionChecker.getUserPermissions(requester);
        // permissions = Set.of("CLIENT_VIEW_OWN", "SESSION_CREATE", ...)

        // Get current principal from SecurityContext (unified auth)
        AuthPrincipal currentUser = permissionChecker.getCurrentAuthPrincipal();
        if (permissionChecker.currentUserHasPermission("AUDIT_VIEW")) {
            // Current user has audit view permission
        }
    }

    // ============================================
    // EXAMPLE 5: Migration from Role-Based to Permission-Based
    // ============================================

    /**
     * OLD WAY (Role-based - DON'T USE):
     * 
     * @PreAuthorize("hasRole('THERAPIST')")
     * public void oldMethod() { ... }
     * 
     * if (hasRole(principal, "THERAPIST")) { ... }
     * 
     * ❌ Roles are hardcoded in code
     * ❌ Cannot add new roles without code changes
     * ❌ Not flexible for enterprise systems
     */

    /**
     * NEW WAY (Permission-based - USE THIS):
     * 
     * @PreAuthorize("hasAuthority('SESSION_CREATE')")
     * public void newMethod() { ... }
     * 
     * if (permissionChecker.hasPermission(principal, "SESSION_CREATE")) { ... }
     * 
     * ✅ Permissions are stored in database
     * ✅ Admins can add permissions via API
     * ✅ No code changes needed
     * ✅ Enterprise-grade flexibility
     */

    // ============================================
    // EXAMPLE 6: Complete Service Method Pattern
    // ============================================

    /**
     * Complete example: Service method with permission + data scope
     * 
     * This is the pattern you should use in your services.
     */
    public void completeExample(
            Long resourceId,
            @AuthenticationPrincipal AuthPrincipal requester
    ) {
        // 1. Check permission (what can user do?)
        permissionChecker.requirePermission(requester, "RESOURCE_VIEW");

        // 2. Load resource
        // Resource resource = repository.findById(resourceId)
        //     .orElseThrow(() -> new ResourceNotFoundException("Not found"));

        // 3. Check data scope (which data can user access?)
        boolean canViewAll = permissionChecker.hasPermission(requester, "RESOURCE_VIEW_ALL");
        boolean canViewOwn = permissionChecker.hasPermission(requester, "RESOURCE_VIEW_OWN");

        if (canViewAll) {
            // No scope check needed
            // return resource;
        } else if (canViewOwn) {
            // Check ownership
            // if (!Objects.equals(resource.getOwnerId(), requester.getId())) {
            //     throw new ForbiddenException("Not your resource");
            // }
            // return resource;
        } else {
            throw new ForbiddenException("No permission to view this resource");
        }
    }
}

