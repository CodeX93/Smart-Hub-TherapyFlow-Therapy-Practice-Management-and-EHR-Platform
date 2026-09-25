package com.smart.therapy.flow.common.security;

import com.smart.therapy.flow.common.exception.ForbiddenException;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Stub break-glass access workflow. Production approval policy and alerting remain operational tasks.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BreakGlassService {

    private final PlatformAuditService platformAuditService;

    @Value("${app.security.break-glass.enabled:false}")
    private boolean enabled;

    public BreakGlassResult requestAccess(Long authId, String reason, String resourceType, String resourceId) {
        if (!enabled) {
            throw new ForbiddenException("Break-glass access is disabled");
        }
        if (!StringUtils.hasText(reason) || reason.trim().length() < 10) {
            throw new ForbiddenException("Break-glass reason must be at least 10 characters");
        }

        String normalizedReason = reason.trim();
        log.warn("Break-glass access requested: authId={}, resourceType={}, resourceId={}",
                authId, resourceType, resourceId);

        platformAuditService.log(
                authId,
                "BREAK_GLASS_ACCESS_REQUESTED",
                resourceType != null ? resourceType : "UNKNOWN",
                resourceId,
                "reasonLength=" + normalizedReason.length()
        );

        return new BreakGlassResult(true, "Break-glass request logged for review");
    }

    public record BreakGlassResult(boolean accepted, String message) {
    }
}
