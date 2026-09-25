package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.exception.ForbiddenException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility for permission checking. Uses AuthPrincipal (unified auth) only.
 *
 * Usage:
 * - In services: permissionChecker.hasPermission(principal, "CLIENT_VIEW_OWN")
 * - In @PreAuthorize: hasAuthority('CLIENT_VIEW_OWN')
 * - Current user: permissionChecker.getCurrentAuthPrincipal()
 */
@Component
@Slf4j
public class PermissionChecker {

    public boolean hasPermission(AuthPrincipal principal, String permissionName) {
        if (principal == null || permissionName == null) return false;
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> authority.equals(permissionName));
    }

    public boolean hasRole(AuthPrincipal principal, String roleName) {
        if (principal == null || roleName == null) return false;
        String normalizedRoleName = roleName.trim();
        if (normalizedRoleName.isEmpty()) return false;
        String roleAuthority = normalizedRoleName.startsWith("ROLE_")
                ? normalizedRoleName
                : "ROLE_" + normalizedRoleName;
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(roleAuthority::equals);
    }

    public void requirePermission(AuthPrincipal principal, String permissionName) {
        if (!hasPermission(principal, permissionName)) {
            throw new ForbiddenException("Permission required: " + permissionName);
        }
    }

    public boolean hasAnyPermission(AuthPrincipal principal, String... permissionNames) {
        if (principal == null || permissionNames == null || permissionNames.length == 0) return false;
        Set<String> userAuthorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (String permissionName : permissionNames) {
            if (userAuthorities.contains(permissionName)) return true;
        }
        return false;
    }

    public boolean hasAllPermissions(AuthPrincipal principal, String... permissionNames) {
        if (principal == null || permissionNames == null || permissionNames.length == 0) return false;
        Set<String> userAuthorities = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
        for (String permissionName : permissionNames) {
            if (!userAuthorities.contains(permissionName)) return false;
        }
        return true;
    }

    public Set<String> getUserPermissions(AuthPrincipal principal) {
        if (principal == null) return Set.of();
        return principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(authority -> !authority.startsWith("ROLE_"))
                .collect(Collectors.toSet());
    }

    /**
     * Current authenticated principal (AuthPrincipal only).
     */
    public AuthPrincipal getCurrentAuthPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return null;
        Object p = authentication.getPrincipal();
        return p instanceof AuthPrincipal ? (AuthPrincipal) p : null;
    }

    public boolean currentUserHasPermission(String permissionName) {
        AuthPrincipal auth = getCurrentAuthPrincipal();
        return auth != null && hasPermission(auth, permissionName);
    }

    public void requireCurrentUserPermission(String permissionName) {
        AuthPrincipal auth = getCurrentAuthPrincipal();
        if (auth == null) throw new ForbiddenException("Authentication required");
        requirePermission(auth, permissionName);
    }

    /**
     * {@code CONSENT_ADMIN_VIEW} is an admin-module proxy: fixed {@code ADMIN}/{@code SUPER_ADMIN}
     * retain access; custom staff need this permission (not the ADMIN role) for tasks, notifications,
     * library admin, assessment builder, checklists, documents, and system/practice config APIs.
     */
    public boolean hasConsentAdminModuleAccess(AuthPrincipal principal) {
        if (principal == null) return false;
        if (hasRole(principal, "ADMIN") || hasRole(principal, "SUPER_ADMIN")) {
            return true;
        }
        return hasPermission(principal, "CONSENT_ADMIN_VIEW");
    }

    public void requireConsentAdminModuleAccess(AuthPrincipal principal, String message) {
        if (!hasConsentAdminModuleAccess(principal)) {
            throw new ForbiddenException(message);
        }
    }

    /** {@code ASSESSMENT_VIEW} or admin-module proxy ({@code CONSENT_ADMIN_VIEW}). */
    public boolean hasAssessmentViewAccess(AuthPrincipal principal) {
        if (principal == null) return false;
        return hasPermission(principal, "ASSESSMENT_VIEW") || hasConsentAdminModuleAccess(principal);
    }

    /** {@code ASSESSMENT_ASSIGN} or admin-module proxy ({@code CONSENT_ADMIN_VIEW}). */
    public boolean hasAssessmentAssignAccess(AuthPrincipal principal) {
        if (principal == null) return false;
        return hasPermission(principal, "ASSESSMENT_ASSIGN") || hasConsentAdminModuleAccess(principal);
    }

    public void requireAssessmentAssignAccess(AuthPrincipal principal, String message) {
        if (!hasAssessmentAssignAccess(principal)) {
            throw new ForbiddenException(message);
        }
    }
}
