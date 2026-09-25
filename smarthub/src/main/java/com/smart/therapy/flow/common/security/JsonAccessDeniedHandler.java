package com.smart.therapy.flow.common.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.exception.AccessDeniedMessageResolver;
import com.smart.therapy.flow.common.exception.ErrorCode;
import com.smart.therapy.flow.common.exception.ErrorResponse;
import com.smart.therapy.flow.common.metrics.AuthAbuseMetrics;
import com.smart.therapy.flow.common.util.HttpRequestUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;
    private final AuditLogService auditLogService;
    private final AuthAbuseMetrics authAbuseMetrics;
    private final ClientRepository clientRepository;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {
        String path = request.getRequestURI();
        String message = AccessDeniedMessageResolver.resolve(request);

        logAccessDenied(path, message);
        auditUnauthorizedAccess(request, path, message);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.FORBIDDEN.value())
                .error("Forbidden")
                .message(message)
                .code(ErrorCode.AUTH_FORBIDDEN.getCode())
                .path(path)
                .traceId(getTraceId())
                .build();

        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setCharacterEncoding("UTF-8");
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), error);
    }

    private void auditUnauthorizedAccess(HttpServletRequest request, String path, String message) {
        try {
            authAbuseMetrics.incrementForbidden();
            AuthPrincipal principal = resolvePrincipal();
            // Staff: login email/username. Client: MRN only (HIPAA).
            String username = resolveAuditUsername(principal);
            String ipAddress = HttpRequestUtil.getClientIp(request);
            String userAgent = HttpRequestUtil.getUserAgent(request);

            Map<String, Object> details = new HashMap<>();
            details.put("method", request.getMethod());
            details.put("message", message);
            details.put("traceId", getTraceId());

            auditLogService.logUnauthorizedAccess(null, username, "endpoint", path, ipAddress, userAgent, details);
        } catch (Exception e) {
            org.slf4j.LoggerFactory.getLogger(JsonAccessDeniedHandler.class)
                    .warn("Failed to record unauthorized_access audit event for path {}: {}", path, e.getMessage());
        }
    }

    private String resolveAuditUsername(AuthPrincipal principal) {
        if (principal == null) {
            return null;
        }
        if (principal.getIdentityType() == IdentityType.CLIENT) {
            try {
                return clientRepository.findByAuthId(principal.getAuthId())
                        .map(HipaaAuditLabels::clientActor)
                        .orElseGet(HipaaAuditLabels::clientActorFallback);
            } catch (Exception e) {
                return HipaaAuditLabels.clientActorFallback();
            }
        }
        return principal.getLoginIdentifier();
    }

    private AuthPrincipal resolvePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthPrincipal authPrincipal)) {
            return null;
        }
        return authPrincipal;
    }

    private void logAccessDenied(String path, String message) {
        org.slf4j.LoggerFactory.getLogger(JsonAccessDeniedHandler.class)
                .warn("Access denied: traceId={}, path={}, message={}", getTraceId(), path, message);
    }

    private String getTraceId() {
        String traceId = MDC.get("correlationId");
        return traceId != null ? traceId : "unknown";
    }
}
