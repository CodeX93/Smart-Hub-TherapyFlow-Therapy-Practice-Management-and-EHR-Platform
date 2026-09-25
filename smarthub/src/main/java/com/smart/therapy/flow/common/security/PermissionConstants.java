package com.smart.therapy.flow.common.security;

/**
 * Centralized permission constants for use in @PreAuthorize expressions.
 *
 * **IMPORTANT: Dynamic Permission System**
 * 
 * Permissions are stored in the database and loaded dynamically. You can:
 * 
 * 1. **Use constants (convenience, but optional):**
 *    @PreAuthorize(PermissionConstants.CLIENT_VIEW_OWN)
 * 
 * 2. **Use dynamic strings (recommended for flexibility):**
 *    @PreAuthorize("hasAuthority('CLIENT_VIEW_OWN')")
 *    // Or from method parameter:
 *    @PreAuthorize("hasAuthority(#permissionName)")
 * 
 * 3. **In service methods, use PermissionChecker:**
 *    if (!permissionChecker.hasPermission(principal, "CLIENT_VIEW_OWN")) {
 *        throw new ForbiddenException("Not allowed");
 *    }
 * 
 * **Adding New Permissions:**
 * - Admins can add new permissions via /api/v1/permissions endpoint
 * - No code changes needed - just use the permission name as a string
 * - Assign permissions to roles via /api/v1/roles/{id}/permissions
 * 
 * Permissions are granted to roles via RolePermission mappings and then
 * exposed as Spring Security authorities by AuthPrincipal.
 *
 * Example usage:
 *  - @PreAuthorize(PermissionConstants.CLIENT_VIEW_OWN)
 *  - @PreAuthorize("hasAuthority('SESSION_CREATE')")  // Dynamic
 *  - @PreAuthorize("hasAuthority(#permissionName)")   // From parameter
 */
public final class PermissionConstants {

    private PermissionConstants() {
        // Utility class
    }

    // ===== User management =====
    public static final String USER_VIEW   = "hasAuthority('USER_VIEW')";
    public static final String USER_CREATE = "hasAuthority('USER_CREATE')";
    public static final String USER_EDIT   = "hasAuthority('USER_EDIT')";
    public static final String USER_DELETE = "hasAuthority('USER_DELETE')";
    public static final String USER_MANAGE = "hasAuthority('USER_MANAGE')";

    // ===== Client management =====
    // Note: These follow the PBAC model with data scope enforcement
    public static final String CLIENT_VIEW_OWN  = "hasAuthority('CLIENT_VIEW_OWN')";
    public static final String CLIENT_VIEW_TEAM = "hasAuthority('CLIENT_VIEW_TEAM')";
    public static final String CLIENT_VIEW_ALL  = "hasAuthority('CLIENT_VIEW_ALL')";
    public static final String CLIENT_VIEW      = "hasAuthority('CLIENT_VIEW')";
    public static final String CLIENT_CREATE     = "hasAuthority('CLIENT_CREATE')";
    public static final String CLIENT_EDIT      = "hasAuthority('CLIENT_EDIT')";
    public static final String CLIENT_DELETE     = "hasAuthority('CLIENT_DELETE')";

    // ===== Session management =====
    public static final String SESSION_VIEW   = "hasAuthority('SESSION_VIEW')";
    public static final String SESSION_CREATE = "hasAuthority('SESSION_CREATE')";
    public static final String SESSION_EDIT   = "hasAuthority('SESSION_EDIT')";
    public static final String SESSION_DELETE = "hasAuthority('SESSION_DELETE')";

    // ===== Assessment management =====
    public static final String ASSESSMENT_VIEW   = "hasAuthority('ASSESSMENT_VIEW')";
    public static final String ASSESSMENT_ASSIGN = "hasAuthority('ASSESSMENT_ASSIGN')";

    // ===== Form/Document management =====
    public static final String FORM_TEMPLATE_MANAGE = "hasAuthority('FORM_TEMPLATE_MANAGE')";
    public static final String FORM_FILL            = "hasAuthority('FORM_FILL')";
    public static final String FORM_VIEW            = "hasAuthority('FORM_VIEW')";

    // ===== Billing management =====
    public static final String BILLING_VIEW   = "hasAuthority('BILLING_VIEW')";
    public static final String BILLING_MANAGE  = "hasAuthority('BILLING_MANAGE')";
    public static final String BILLING_CREATE  = "hasAuthority('BILLING_CREATE')";
    public static final String BILLING_EDIT    = "hasAuthority('BILLING_EDIT')";
    public static final String BILLING_DELETE  = "hasAuthority('BILLING_DELETE')";
    public static final String BILLING_EXPORT  = "hasAuthority('BILLING_EXPORT')";
    /** Read billing data (statistics, history, services). Includes custom tenant roles. */
    public static final String BILLING_READ_ACCESS = "hasAnyAuthority('BILLING_VIEW','BILLING_MANAGE')";
    /** Mutate billing records (payments, discounts, status). */
    public static final String BILLING_WRITE_ACCESS = "hasAnyAuthority('BILLING_EDIT','BILLING_MANAGE')";
    public static final String BILLING_CREATE_ACCESS = "hasAnyAuthority('BILLING_CREATE','BILLING_MANAGE')";
    public static final String BILLING_EXPORT_ACCESS = "hasAnyAuthority('BILLING_EXPORT','BILLING_MANAGE')";
    public static final String BILLING_DELETE_ACCESS = "hasAnyAuthority('BILLING_DELETE','BILLING_MANAGE')";
    /** Any billing module permission — used for URL-level /api/v1/billing/** access. */
    public static final String BILLING_MODULE_ACCESS =
            "hasAnyAuthority('BILLING_VIEW','BILLING_MANAGE','BILLING_CREATE','BILLING_EDIT','BILLING_DELETE','BILLING_EXPORT')";

    // ===== Reporting =====
    public static final String REPORT_VIEW   = "hasAuthority('REPORT_VIEW')";
    public static final String REPORT_EXPORT = "hasAuthority('REPORT_EXPORT')";

    // ===== Client exports =====
    public static final String CLIENT_EXPORT = "hasAuthority('CLIENT_EXPORT')";

    // ===== Audit & compliance =====
    public static final String AUDIT_VIEW   = "hasAuthority('AUDIT_VIEW')";
    public static final String AUDIT_EXPORT = "hasAuthority('AUDIT_EXPORT')";

    // ===== Room management =====
    public static final String ROOM_MANAGE = "hasAuthority('ROOM_MANAGE')";
    /** Read rooms / availability for scheduling or room administration. */
    public static final String ROOM_READ_ACCESS =
            "hasAnyAuthority('ROOM_MANAGE','SESSION_VIEW','SESSION_CREATE','SESSION_EDIT','CONSENT_ADMIN_VIEW')";

    // ===== Consents (admin view) =====
    /**
     * Admin-module proxy permission. When granted to a custom staff role, unlocks secondary admin
     * surfaces (tasks, notifications, library admin, assessment builder, checklists, documents,
     * system/practice config) — same APIs tenant admin uses, without requiring the ADMIN role.
     * Fixed portal roles still require this permission where applicable.
     */
    public static final String CONSENT_ADMIN_VIEW = "hasAuthority('CONSENT_ADMIN_VIEW')";
    /** Alias documenting proxy intent; equivalent to {@link #CONSENT_ADMIN_VIEW} for @PreAuthorize. */
    public static final String CONSENT_ADMIN_MODULE_ACCESS = CONSENT_ADMIN_VIEW;

    // ===== AI features =====
    public static final String AI_USE = "hasAuthority('AI_USE')";

    // ===== Platform (super-admin) control plane =====
    public static final String PLATFORM_READ = "hasAuthority('PLATFORM_READ')";
    public static final String PLATFORM_MANAGE = "hasAuthority('PLATFORM_MANAGE')";

    // ===== Client Portal Permissions =====
    // Note: Clients use ROLE_CLIENT for authentication, but can have permissions too
    // These permissions are for client-specific actions within the portal
    public static final String CLIENT_PORTAL_ACCESS = "hasAuthority('CLIENT_PORTAL_ACCESS')";
    public static final String CLIENT_VIEW_OWN_PROFILE = "hasAuthority('CLIENT_VIEW_OWN_PROFILE')";
    public static final String CLIENT_EDIT_OWN_PROFILE = "hasAuthority('CLIENT_EDIT_OWN_PROFILE')";
    public static final String CLIENT_VIEW_OWN_ASSESSMENTS = "hasAuthority('CLIENT_VIEW_OWN_ASSESSMENTS')";
    public static final String CLIENT_SUBMIT_ASSESSMENTS = "hasAuthority('CLIENT_SUBMIT_ASSESSMENTS')";
    public static final String CLIENT_VIEW_OWN_SESSIONS = "hasAuthority('CLIENT_VIEW_OWN_SESSIONS')";
    public static final String CLIENT_BOOK_APPOINTMENTS = "hasAuthority('CLIENT_BOOK_APPOINTMENTS')";
    public static final String CLIENT_VIEW_OWN_DOCUMENTS = "hasAuthority('CLIENT_VIEW_OWN_DOCUMENTS')";
    public static final String CLIENT_VIEW_OWN_BILLING = "hasAuthority('CLIENT_VIEW_OWN_BILLING')";
}
