package com.smart.therapy.flow.common.tenant;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;

import java.io.IOException;
import java.util.List;

/**
 * When request uses public schema only (no tenant), allow ONLY:
 * - auth, public, health, swagger, super-admin.
 * Reject all other API paths with 403 to prevent tenant APIs running against public (data leak).
 */
@Component
@Order(2)
@Slf4j
public class PublicSchemaGuardFilter implements Filter {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> ALLOWED_PATTERNS = List.of(
            "/api/v1/auth/**",
            "/api/v1/portal/login",
            "/api/v1/portal/login-context",
            "/api/v1/portal/mfa/verify-login",
            "/api/v1/portal/refresh",
            "/api/v1/portal/activate",
            "/api/v1/portal/activate/validate",
            "/api/v1/portal/forgot-password",
            "/api/v1/portal/reset-password",
            "/ws/transcribe-live",
            "/api/public/**",
            "/api/v1/public/**",
            "/api/v1/super-admin/**",
            "/api/super-admin/features/catalog/**",
            "/api/super-admin/catalog/addons/**",
            "/api/super-admin/plans/**",
            "/actuator/health",
            "/actuator/health/**",
            "/.well-known/acme-challenge/**",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/swagger-resources/**",
            // Stripe Connect OAuth callback (Stripe redirect has no tenant subdomain; org resolved via oauth state)
            "/api/v1/admin/stripe-connect/oauth/callback",
            // Stripe webhooks (secured by Stripe-Signature, not tenant context)
            "/api/v1/stripe/webhook/**"
    );

    private static final String FORBIDDEN_JSON = "{\"error\":\"Public schema cannot access this API. Use a tenant subdomain or super-admin endpoints.\",\"code\":\"PUBLIC_SCHEMA_FORBIDDEN\"}";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        String schema = TenantContext.getSchemaName();
        boolean isPublicSchema = schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);

        if (!isPublicSchema) {
            chain.doFilter(request, response);
            return;
        }

        String path = req.getRequestURI();
        boolean allowed = ALLOWED_PATTERNS.stream().anyMatch(p -> MATCHER.match(p, path));

        if (allowed) {
            chain.doFilter(request, response);
            return;
        }

        log.warn("Blocked public-schema access to tenant API: {} {}", req.getMethod(), path);
        res.setStatus(HttpServletResponse.SC_FORBIDDEN);
        res.setContentType("application/json");
        res.setCharacterEncoding("UTF-8");
        res.getWriter().write(FORBIDDEN_JSON);
    }
}
