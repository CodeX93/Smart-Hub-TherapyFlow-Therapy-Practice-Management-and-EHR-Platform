package com.smart.therapy.flow.common.logging;

import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.common.service.GlobalActivityAuditService;
import com.smart.therapy.flow.common.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class GlobalActivityAuditAspect {

    private static final Pattern CLIENT_PATH_ID = Pattern.compile("/clients/(\\d+)");

    private final GlobalActivityAuditService globalActivityAuditService;
    private final SensitiveDataMasker sensitiveDataMasker;

    @Pointcut("within(com.smart.therapy.flow..controller..*)")
    public void allControllers() {
    }

    @Around("allControllers()")
    public Object auditAllControllerActions(ProceedingJoinPoint joinPoint) throws Throwable {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        HttpServletRequest request = attributes != null ? attributes.getRequest() : null;

        if (request == null || shouldSkip(request)) {
            return joinPoint.proceed();
        }

        long startNanos = System.nanoTime();
        int statusCode = 200;
        Object result = null;
        Throwable error = null;

        try {
            result = joinPoint.proceed();
            statusCode = resolveStatusCode(result);
            return result;
        } catch (Throwable ex) {
            error = ex;
            statusCode = 500;
            throw ex;
        } finally {
            try {
                recordAuditEvent(joinPoint, request, statusCode, startNanos, error);
            } catch (Exception loggingEx) {
                log.warn("Global activity audit failed: {}", loggingEx.getMessage());
            }
        }
    }

    private void recordAuditEvent(ProceedingJoinPoint joinPoint,
                                  HttpServletRequest request,
                                  int statusCode,
                                  long startNanos,
                                  Throwable error) {
        AuthPrincipal principal = resolvePrincipal();
        String method = request.getMethod();
        String path = request.getRequestURI();
        String action = resolveAction(method, path);
        String ipAddress = resolveClientIp(request);
        String userAgent = request.getHeader("User-Agent");
        long durationMs = (System.nanoTime() - startNanos) / 1_000_000;
        Long inferredClientId = extractClientId(path);

        String details = sensitiveDataMasker.createAuditDetails(joinPoint.getArgs(), error, 1200);

        boolean tenantScoped = isTenantScoped();
        if (tenantScoped) {
            boolean hipaaRelevant = isHipaaRelevantPath(path);
            globalActivityAuditService.recordTenantApiActivity(
                    principal, action, path, method, statusCode, ipAddress, userAgent,
                    inferredClientId, durationMs, details, hipaaRelevant);
        } else {
            globalActivityAuditService.recordPlatformApiActivity(
                    principal, action, path, method, statusCode, details, durationMs);
        }
    }

    private AuthPrincipal resolvePrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof AuthPrincipal authPrincipal) {
            return authPrincipal;
        }
        return null;
    }

    private boolean isTenantScoped() {
        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        return StringUtils.hasText(schema) && !"public".equalsIgnoreCase(schema) && orgId != null;
    }

    private boolean shouldSkip(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (!StringUtils.hasText(path)) {
            return true;
        }
        
        if ("GET".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        
        String normalized = path.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("/actuator")
                || normalized.startsWith("/swagger-ui")
                || normalized.startsWith("/v3/api-docs")
                || normalized.contains("/auth/refresh")) {
            return true;
        }

        if ("POST".equalsIgnoreCase(request.getMethod()) && normalized.matches("^/api/v1/(clients/[^/]+/portal/)?sessions$")) {
            return true; // Exclude session creation from generic logging to prevent duplicates
        }

        return false;
    }

    private int resolveStatusCode(Object result) {
        if (result instanceof ResponseEntity<?> responseEntity) {
            return responseEntity.getStatusCode().value();
        }
        return 200;
    }

    private String resolveAction(String method, String path) {
        String normalizedPath = (path != null ? path : "/unknown");
        return method.toUpperCase() + " " + normalizedPath;
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp)) {
            return xRealIp;
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "127.0.0.1";
    }

    private Long extractClientId(String path) {
        if (!StringUtils.hasText(path)) {
            return null;
        }
        Matcher matcher = CLIENT_PATH_ID.matcher(path);
        if (!matcher.find()) {
            return null;
        }
        try {
            return Long.parseLong(matcher.group(1));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isHipaaRelevantPath(String path) {
        if (!StringUtils.hasText(path)) {
            return false;
        }
        String normalized = path.toLowerCase(Locale.ROOT);
        return normalized.contains("/clients")
                || normalized.contains("/sessions")
                || normalized.contains("/session-notes")
                || normalized.contains("/documents")
                || normalized.contains("/notes")
                || normalized.contains("/forms")
                || normalized.contains("/assessments")
                || normalized.contains("/portal")
                || normalized.contains("/consent")
                || normalized.contains("/checklists")
                || normalized.contains("/tasks");
    }
}
