package com.smart.therapy.flow.common.security;

/**
 * Role constants for RBAC
 * Use these constants with @PreAuthorize annotations
 */
public class RoleConstants {
    /** Platform (control-plane) role: access admin.yourapp.com without tenant. */
    public static final String ROLE_PLATFORM_SUPER_ADMIN = "hasRole('PLATFORM_SUPER_ADMIN')";
    /** Platform read-only role: list orgs, view health, audit logs; no create/update/delete/provision. */
    public static final String ROLE_PLATFORM_AUDITOR = "hasRole('PLATFORM_AUDITOR')";
    /** Super-admin or Auditor (for read-only platform endpoints). */
    public static final String PLATFORM_READ = "hasAnyRole('PLATFORM_SUPER_ADMIN', 'PLATFORM_AUDITOR')";
    public static final String ROLE_SUPER_ADMIN = "hasRole('SUPER_ADMIN')";
    public static final String ROLE_ADMIN = "hasRole('ADMIN')";
    public static final String ROLE_THERAPIST = "hasRole('THERAPIST')";
    public static final String ROLE_CLIENT = "hasRole('CLIENT')";
    public static final String ROLE_SUPERVISOR = "hasRole('SUPERVISOR')";
    public static final String ROLE_BILLING_SPECIALIST = "hasRole('BILLING_SPECIALIST')";
    public static final String ROLE_SYSTEM_AI_ASSISTANT = "hasRole('SYSTEM_AI_ASSISTANT')";
    
    // Combined roles
    public static final String ADMIN_OR_SUPER_ADMIN = "hasAnyRole('ADMIN', 'SUPER_ADMIN')";
    public static final String THERAPIST_OR_ADMIN = "hasAnyRole('THERAPIST', 'ADMIN', 'SUPERVISOR')";
    public static final String CLIENT_OR_THERAPIST = "hasAnyRole('CLIENT', 'THERAPIST', 'ADMIN')";
}

