package com.smart.therapy.flow.common.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Resolves impersonation context from the active security principal.
 */
public final class ImpersonationContext {

    private ImpersonationContext() {
    }

    public static Long getImpersonatorAuthId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal.getImpersonatorAuthId();
        }
        return null;
    }
}
