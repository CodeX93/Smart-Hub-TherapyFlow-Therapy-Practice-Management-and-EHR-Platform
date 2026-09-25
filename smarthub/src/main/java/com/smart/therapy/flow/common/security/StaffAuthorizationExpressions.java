package com.smart.therapy.flow.common.security;

/**
 * SpEL for tenant staff API authorization.
 *
 * <p>Fixed portal roles ({@code ADMIN}, {@code THERAPIST}, {@code SUPERVISOR}, etc.) keep the
 * legacy {@code hasRole(...) and hasAuthority(...)} gate unchanged.
 *
 * <p>Custom tenant roles (e.g. {@code BILLING_ROLE}) are authorized by permission only when the
 * user does not hold a fixed portal role — see {@code docs/custom-staff-authorization.md}.
 *
 * <p>All values must be compile-time constants for use in {@code @PreAuthorize}.
 */
public final class StaffAuthorizationExpressions {

    private StaffAuthorizationExpressions() {
    }

    /** Fixed tenant portal roles — users with these use dedicated portals, not custom staff. */
    public static final String FIXED_TENANT_PORTAL_ROLES =
            "'SUPER_ADMIN','ADMIN','THERAPIST','SUPERVISOR','CLIENT'";

    public static final String NOT_FIXED_TENANT_PORTAL_ROLE =
            "!hasAnyRole(" + FIXED_TENANT_PORTAL_ROLES + ")";

    public static final String CLIENT_VIEW_ANY =
            "hasAnyAuthority('CLIENT_VIEW','CLIENT_VIEW_ALL','CLIENT_VIEW_OWN','CLIENT_VIEW_TEAM')";

    // ===== Billing =====

    public static final String BILLING_READ =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR','THERAPIST') and "
                    + PermissionConstants.BILLING_VIEW + ") or (hasAnyAuthority('BILLING_VIEW','BILLING_MANAGE') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String BILLING_READ_NO_THERAPIST =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR') and "
                    + PermissionConstants.BILLING_VIEW + ") or (hasAnyAuthority('BILLING_VIEW','BILLING_MANAGE') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String BILLING_WRITE =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR') and "
                    + PermissionConstants.BILLING_EDIT + ") or (hasAnyAuthority('BILLING_EDIT','BILLING_MANAGE') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String BILLING_CREATE_ACCESS =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR','THERAPIST') and "
                    + PermissionConstants.BILLING_CREATE + ") or (hasAnyAuthority('BILLING_CREATE','BILLING_MANAGE') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String BILLING_EXPORT_ACCESS =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN','SUPERVISOR','THERAPIST') and "
                    + PermissionConstants.BILLING_EXPORT + ") or (hasAnyAuthority('BILLING_EXPORT','BILLING_MANAGE') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String BILLING_MANAGE_ACCESS =
            "((hasAnyRole('BILLING_SPECIALIST','ADMIN') and "
                    + PermissionConstants.BILLING_MANAGE + ") or ("
                    + PermissionConstants.BILLING_MANAGE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    // ===== Clients =====

    public static final String CLIENT_READ =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_VIEW
                    + ") or (" + CLIENT_VIEW_ANY + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String CLIENT_CREATE_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_CREATE
                    + ") or (" + PermissionConstants.CLIENT_CREATE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String CLIENT_EDIT_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_EDIT
                    + ") or (" + PermissionConstants.CLIENT_EDIT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String CLIENT_EXPORT_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_EXPORT
                    + ") or (" + PermissionConstants.CLIENT_EXPORT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_CLIENT_READ =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_VIEW
                    + ") or (" + CLIENT_VIEW_ANY + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_CLIENT_CREATE =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_CREATE
                    + ") or (" + PermissionConstants.CLIENT_CREATE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_CLIENT_EDIT =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.CLIENT_EDIT
                    + ") or (" + PermissionConstants.CLIENT_EDIT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    // ===== Sessions =====

    public static final String SESSION_READ =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.SESSION_VIEW
                    + ") or (" + PermissionConstants.SESSION_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String SESSION_CREATE_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.SESSION_CREATE
                    + ") or (" + PermissionConstants.SESSION_CREATE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String SESSION_EDIT_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.SESSION_EDIT
                    + ") or (" + PermissionConstants.SESSION_EDIT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String SESSION_DELETE_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.SESSION_DELETE
                    + ") or (" + PermissionConstants.SESSION_DELETE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_SESSION_CREATE =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.SESSION_CREATE
                    + ") or (" + PermissionConstants.SESSION_CREATE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    /**
     * Admin-module proxy gate ({@code CONSENT_ADMIN_VIEW}). Fixed portal roles need the permission;
     * custom staff roles are authorized by permission only.
     */
    public static final String CONSENT_ADMIN_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.CONSENT_ADMIN_VIEW
                    + ") or (" + PermissionConstants.CONSENT_ADMIN_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    /** Same as {@link #CONSENT_ADMIN_ACCESS} — use on endpoints behind the admin-module proxy. */
    public static final String CONSENT_ADMIN_MODULE_ACCESS = CONSENT_ADMIN_ACCESS;

    // ===== Assessments =====

    /** View assessments, assignments, reports. {@code CONSENT_ADMIN_VIEW} is an admin proxy alternative. */
    public static final String ASSESSMENT_READ_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and ("
                    + PermissionConstants.ASSESSMENT_VIEW + " or " + PermissionConstants.CONSENT_ADMIN_VIEW
                    + ")) or (hasAnyAuthority('ASSESSMENT_VIEW','CONSENT_ADMIN_VIEW') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    /** Assign assessments, submit responses, manage templates. {@code CONSENT_ADMIN_VIEW} is an admin proxy alternative. */
    public static final String ASSESSMENT_ASSIGN_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and ("
                    + PermissionConstants.ASSESSMENT_ASSIGN + " or " + PermissionConstants.CONSENT_ADMIN_VIEW
                    + ")) or (hasAnyAuthority('ASSESSMENT_ASSIGN','CONSENT_ADMIN_VIEW') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    // ===== Forms =====

    /** Read form templates and assignments. {@code CONSENT_ADMIN_VIEW} is an admin proxy alternative. */
    public static final String FORM_READ_ACCESS =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and ("
                    + PermissionConstants.FORM_VIEW + " or " + PermissionConstants.CONSENT_ADMIN_VIEW
                    + ")) or (hasAnyAuthority('FORM_VIEW','CONSENT_ADMIN_VIEW') and "
                    + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    // ===== Users (library / directory reads) =====

    public static final String ADMIN_SUPERVISOR_USER_READ =
            "((" + RoleConstants.ADMIN_OR_SUPER_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR
                    + ") and " + PermissionConstants.USER_VIEW
                    + " or (" + PermissionConstants.USER_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String THERAPIST_ADMIN_USER_READ =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.USER_VIEW
                    + ") or (" + PermissionConstants.USER_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    /**
     * Read peer working-hours / profile for scheduling. Service enforces own-or-caseload.
     * Allows SESSION_VIEW / CLIENT_VIEW_TEAM without USER_VIEW (supervisors after lockdown).
     * Fixed portal roles (incl. SUPERVISOR) use the role+permission branch; custom staff use
     * permission-only when they are not a fixed portal role.
     */
    public static final String SCHEDULING_USER_PROFILE_READ =
            "((hasAnyRole('ADMIN','SUPERVISOR','THERAPIST','SUPER_ADMIN') and ("
                    + PermissionConstants.USER_VIEW
                    + " or " + PermissionConstants.SESSION_VIEW
                    + " or " + PermissionConstants.CLIENT_VIEW_TEAM
                    + ")) or (("
                    + PermissionConstants.USER_VIEW
                    + " or " + PermissionConstants.SESSION_VIEW
                    + " or " + PermissionConstants.CLIENT_VIEW_TEAM
                    + ") and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String THERAPIST_ADMIN_USER_EDIT =
            "((hasAnyRole('THERAPIST','ADMIN','SUPERVISOR') and " + PermissionConstants.USER_EDIT
                    + ") or (" + PermissionConstants.USER_EDIT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String USER_EDIT_OR_MANAGE = "hasAnyAuthority('USER_EDIT','USER_MANAGE')";
    public static final String USER_CREATE_OR_MANAGE = "hasAnyAuthority('USER_CREATE','USER_MANAGE')";
    public static final String USER_DELETE_OR_MANAGE = "hasAnyAuthority('USER_DELETE','USER_MANAGE')";

    public static final String ADMIN_SUPERVISOR_USER_CREATE =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.USER_CREATE
                    + ") or (" + USER_CREATE_OR_MANAGE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_USER_EDIT =
            "((hasAnyRole('ADMIN','SUPERVISOR') and " + PermissionConstants.USER_EDIT
                    + ") or (" + USER_EDIT_OR_MANAGE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String ADMIN_SUPERVISOR_USER_DELETE =
            "((" + RoleConstants.ROLE_ADMIN + " and " + PermissionConstants.USER_DELETE
                    + ") or (" + USER_DELETE_OR_MANAGE + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    // ===== Audit / compliance =====

    public static final String AUDIT_READ =
            "(" + RoleConstants.ROLE_ADMIN
                    + " or (" + RoleConstants.ROLE_SUPERVISOR + " and " + PermissionConstants.AUDIT_VIEW + ")"
                    + " or (" + PermissionConstants.AUDIT_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String AUDIT_EXPORT =
            "((" + RoleConstants.ROLE_ADMIN + " or " + RoleConstants.ROLE_SUPERVISOR
                    + ") and " + PermissionConstants.AUDIT_EXPORT
                    + " or (" + PermissionConstants.AUDIT_EXPORT + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";

    public static final String AUDIT_READ_THERAPIST_OR_ADMIN =
            "(" + RoleConstants.ROLE_ADMIN
                    + " or ((" + RoleConstants.ROLE_SUPERVISOR + " or " + RoleConstants.ROLE_THERAPIST + ") and "
                    + PermissionConstants.AUDIT_VIEW + ")"
                    + " or (" + PermissionConstants.AUDIT_VIEW + " and " + NOT_FIXED_TENANT_PORTAL_ROLE + "))";
}
