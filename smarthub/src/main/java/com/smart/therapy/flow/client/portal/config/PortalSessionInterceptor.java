package com.smart.therapy.flow.client.portal.config;

import com.smart.therapy.flow.client.entity.ClientPortalSession;
import com.smart.therapy.flow.client.repository.ClientPortalSessionRepository;
import com.smart.therapy.flow.common.exception.UnauthorizedException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Instant;

@Component
@RequiredArgsConstructor
public class PortalSessionInterceptor implements HandlerInterceptor {

    private final ClientPortalSessionRepository sessionRepository;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip authentication for public endpoints
        String path = request.getRequestURI();
        if (path.equals("/api/v1/portal/login") ||
                path.equals("/api/v1/portal/login-context") ||
                path.equals("/api/v1/portal/mfa/verify-login") ||
                path.equals("/api/v1/portal/refresh") ||
                path.equals("/api/v1/portal/activate") ||
                path.startsWith("/api/v1/portal/activate/validate") ||
                path.equals("/api/v1/portal/forgot-password") ||
                path.equals("/api/v1/portal/reset-password")) {
            return true;
        }

        // Get session token from cookie
        String sessionToken = getSessionToken(request);
        if (sessionToken == null) {
            throw new UnauthorizedException("Not authenticated");
        }

        // Validate session
        ClientPortalSession session = sessionRepository.findBySessionToken(sessionToken)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired session"));

        if (!Boolean.TRUE.equals(session.getIsActive()) ||
                session.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Session expired");
        }

        // Update session activity
        session.setLastActivity(Instant.now());
        sessionRepository.save(session);

        // Store client ID in request attribute for use in controllers
        if (session.getClient() != null) {
            request.setAttribute("clientId", session.getClient().getId());
        }

        return true;
    }

    private String getSessionToken(HttpServletRequest request) {
        // First check for session token in header (for Swagger UI/testing/API clients)
        String headerToken = request.getHeader("X-Portal-Session-Token");
        if (headerToken != null && !headerToken.isEmpty()) {
            return headerToken;
        }

        // Then check cookies (for normal browser usage)
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("portalSessionToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
