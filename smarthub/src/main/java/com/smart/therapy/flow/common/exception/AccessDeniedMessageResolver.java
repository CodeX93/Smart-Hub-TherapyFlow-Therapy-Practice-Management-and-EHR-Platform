package com.smart.therapy.flow.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AccessDeniedMessageResolver {

    private AccessDeniedMessageResolver() {
    }

    public static String resolve(HttpServletRequest request) {
        String path = request != null ? request.getRequestURI() : null;
        if (path == null) {
            return "You do not have permission to access this resource.";
        }

        if (isTherapistUser()) {
            String therapistConsentMessage = getTherapistConsentAccessDeniedMessage(path, request.getMethod());
            if (therapistConsentMessage != null) {
                return therapistConsentMessage;
            }
        }

        if (path.startsWith("/api/v1/users/") && !path.equals("/api/v1/users/me") && !path.startsWith("/api/v1/users/me/")) {
            return "You do not have permission to access other users' information. Only administrators and supervisors can view user details. Use /api/v1/users/me to access your own profile.";
        }

        if (path.startsWith("/api/v1/admin/")) {
            return "You do not have permission to access administrative functions. Administrator role is required.";
        }

        if (path.startsWith("/api/v1/roles/") || path.startsWith("/api/v1/permissions/")) {
            return "You do not have permission to manage roles and permissions. Administrator role is required.";
        }

        return "You do not have permission to access this resource. Please contact your administrator if you believe this is an error.";
    }

    private static String getTherapistConsentAccessDeniedMessage(String path, String method) {
        if (!path.startsWith("/api/v1/admin/consents")) {
            return null;
        }

        if ("POST".equalsIgnoreCase(method) && path.matches("/api/v1/admin/consents/clients/\\d+")) {
            return "Therapists cannot record client consent from admin consent management. "
                    + "Only administrators and supervisors can record consent on behalf of a client.";
        }

        if ("POST".equalsIgnoreCase(method) && path.matches("/api/v1/admin/consents/clients/\\d+/verbal-ai-consent")) {
            return "Therapists cannot record verbal AI consent from admin consent management. "
                    + "Please contact an administrator or supervisor to update this client's consent.";
        }

        return "Therapists do not have access to admin consent management. "
                + "Only administrators and supervisors can manage client consents from this section.";
    }

    private static boolean isTherapistUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(authority -> "ROLE_THERAPIST".equals(authority) || "THERAPIST".equals(authority));
    }
}
