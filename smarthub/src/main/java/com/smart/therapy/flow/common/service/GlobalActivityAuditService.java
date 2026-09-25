package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.auth.entity.AuditLog;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuditLogRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.audit.service.AuditLogService;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.audit.HipaaAuditLabels;
import com.smart.therapy.flow.common.security.AuthPrincipal;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class GlobalActivityAuditService {

    public static final String GLOBAL_SOURCE_MARKER = "source=global_activity_aspect";

    private final AuditLogRepository auditLogRepository;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ClientRepository clientRepository;
    private final PlatformAuditService platformAuditService;
    private final PlatformAuditLogRepository platformAuditLogRepository;

    @Value("${audit.global.enabled:true}")
    private boolean globalAuditEnabled;

    @Value("${audit.global.tenant.enabled:true}")
    private boolean tenantGlobalAuditEnabled;

    @Value("${audit.global.platform.enabled:true}")
    private boolean platformGlobalAuditEnabled;

    @Value("${audit.global.dedup.enabled:true}")
    private boolean dedupEnabled;

    @Value("${audit.global.dedup.window-seconds:2}")
    private int dedupWindowSeconds;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordTenantApiActivity(AuthPrincipal principal,
                                        String action,
                                        String path,
                                        String method,
                                        int statusCode,
                                        String ipAddress,
                                        String userAgent,
                                        Long inferredClientId,
                                        long durationMs,
                                        String details,
                                        boolean hipaaRelevant) {
        if (!globalAuditEnabled || !tenantGlobalAuditEnabled) {
            return;
        }
        try {
            Instant now = Instant.now();
            String username = resolveTenantActorUsername(principal);
            String result = resolveResult(statusCode);
            String resourceType = resolveResourceType(path);
            String resourceId = extractResourceId(path);
            String normalizedIp = ipAddress != null ? ipAddress : "127.0.0.1";

            if (dedupEnabled) {
                Instant since = now.minusSeconds(Math.max(dedupWindowSeconds, 1));
                boolean duplicate = auditLogRepository.existsRecentGlobalDuplicate(
                        since,
                        username,
                        action,
                        resourceType,
                        resourceId,
                        result,
                        normalizedIp,
                        GLOBAL_SOURCE_MARKER);
                if (duplicate) {
                    return;
                }
            }

            AuditLog auditLog = AuditLog.builder()
                    .username(username)
                    .action(action)
                    .result(result)
                    .resourceType(resourceType)
                    .resourceId(resourceId)
                    .ipAddress(normalizedIp)
                    .userAgent(userAgent)
                    .hipaaRelevant(hipaaRelevant)
                    .riskLevel(resolveRiskLevel(statusCode, hipaaRelevant))
                    .details(detailsWithContext(details, method, path, statusCode, durationMs))
                    .timestamp(now)
                    .build();

            if (principal != null) {
                if (principal.getIdentityType() == IdentityType.STAFF) {
                    userRepository.findByAuthId(principal.getAuthId()).ifPresent(auditLog::setUser);
                } else if (principal.getIdentityType() == IdentityType.CLIENT) {
                    clientRepository.findByAuthId(principal.getAuthId()).ifPresent(auditLog::setClient);
                }
            }

            if (auditLog.getClient() == null && inferredClientId != null) {
                clientRepository.findByIdIncludingDeleted(inferredClientId).ifPresent(auditLog::setClient);
            }

            // If client was attached after username resolution, prefer MRN in the User column
            if (auditLog.getClient() != null
                    && principal != null
                    && principal.getIdentityType() == IdentityType.CLIENT) {
                auditLog.setUsername(HipaaAuditLabels.clientActor(auditLog.getClient()));
            }

            auditLogService.writeImmediate(auditLog);
        } catch (Exception ex) {
            log.warn("Failed to record global tenant API audit activity for action={} path={}: {}", action, path, ex.getMessage());
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordPlatformApiActivity(AuthPrincipal principal,
                                          String action,
                                          String path,
                                          String method,
                                          int statusCode,
                                          String details,
                                          long durationMs) {
        if (!globalAuditEnabled || !platformGlobalAuditEnabled) {
            return;
        }
        try {
            Long authId = principal != null ? principal.getAuthId() : null;
            String payload = detailsWithContext(details, method, path, statusCode, durationMs);
            platformAuditService.log(authId, action, "API", path, payload);
        } catch (Exception ex) {
            log.warn("Failed to record global platform API audit activity for action={} path={}: {}", action, path, ex.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTenantGlobalAuditHealth(int hours) {
        return getTenantGlobalAuditHealth(hours, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getTenantGlobalAuditHealth(int hours, Integer topN) {
        int safeHours = Math.max(hours, 1);
        Instant since = Instant.now().minusSeconds((long) safeHours * 3600);
        Integer safeTopN = (topN != null && topN > 0) ? topN : null;

        long errors = auditLogRepository.countRecentGlobalErrors(since, GLOBAL_SOURCE_MARKER);
        Map<String, Long> allByResource = toCountMap(auditLogRepository.countRecentGlobalByResourceType(since, GLOBAL_SOURCE_MARKER));
        Map<String, Long> allByAction = toCountMap(auditLogRepository.countRecentGlobalByAction(since, GLOBAL_SOURCE_MARKER));
        long total = allByAction.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> byResource = limitTopN(allByResource, safeTopN);
        Map<String, Long> byAction = limitTopN(allByAction, safeTopN);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", "tenant");
        response.put("globalAuditEnabled", globalAuditEnabled && tenantGlobalAuditEnabled);
        response.put("dedupEnabled", dedupEnabled);
        response.put("dedupWindowSeconds", dedupWindowSeconds);
        response.put("hours", safeHours);
        response.put("topN", safeTopN);
        response.put("since", since);
        response.put("totalEvents", total);
        response.put("errorEvents", errors);
        response.put("countsByResourceType", byResource);
        response.put("countsByAction", byAction);
        return response;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformGlobalAuditHealth(int hours) {
        return getPlatformGlobalAuditHealth(hours, null);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getPlatformGlobalAuditHealth(int hours, Integer topN) {
        int safeHours = Math.max(hours, 1);
        Instant since = Instant.now().minusSeconds((long) safeHours * 3600);
        Integer safeTopN = (topN != null && topN > 0) ? topN : null;

        long errors = platformAuditLogRepository.countRecentGlobalErrors(since, GLOBAL_SOURCE_MARKER);
        Map<String, Long> allByResource = toCountMap(platformAuditLogRepository.countRecentGlobalByResourceType(since, GLOBAL_SOURCE_MARKER));
        Map<String, Long> allByAction = toCountMap(platformAuditLogRepository.countRecentGlobalByAction(since, GLOBAL_SOURCE_MARKER));
        long total = allByAction.values().stream().mapToLong(Long::longValue).sum();
        Map<String, Long> byResource = limitTopN(allByResource, safeTopN);
        Map<String, Long> byAction = limitTopN(allByAction, safeTopN);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("scope", "platform");
        response.put("globalAuditEnabled", globalAuditEnabled && platformGlobalAuditEnabled);
        response.put("dedupEnabled", dedupEnabled);
        response.put("hours", safeHours);
        response.put("topN", safeTopN);
        response.put("since", since);
        response.put("totalEvents", total);
        response.put("errorEvents", errors);
        response.put("countsByResourceType", byResource);
        response.put("countsByAction", byAction);
        return response;
    }

    private String resolveTenantActorUsername(AuthPrincipal principal) {
        if (principal == null) {
            return "anonymous";
        }
        if (principal.getIdentityType() == IdentityType.CLIENT) {
            return clientRepository.findByAuthId(principal.getAuthId())
                    .map(HipaaAuditLabels::clientActor)
                    .orElseGet(HipaaAuditLabels::clientActorFallback);
        }
        return principal.getLoginIdentifier() != null ? principal.getLoginIdentifier() : "anonymous";
    }

    private String resolveResult(int statusCode) {
        if (statusCode >= 400) {
            return "failure";
        }
        return "success";
    }

    private String resolveRiskLevel(int statusCode, boolean hipaaRelevant) {
        if (statusCode >= 500) {
            return "high";
        }
        if (statusCode >= 400) {
            return "medium";
        }
        return hipaaRelevant ? "medium" : "low";
    }

    private String resolveResourceType(String path) {
        if (path == null || path.isBlank()) {
            return "api";
        }
        String[] parts = path.split("/");
        for (String part : parts) {
            if (part == null || part.isBlank()) {
                continue;
            }
            if ("api".equalsIgnoreCase(part) || "v1".equalsIgnoreCase(part)) {
                continue;
            }
            return part.toLowerCase(Locale.ROOT);
        }
        return "api";
    }

    private String extractResourceId(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String[] parts = path.split("/");
        for (int i = 0; i < parts.length; i++) {
            String current = parts[i];
            if ("clients".equalsIgnoreCase(current)
                    || "sessions".equalsIgnoreCase(current)
                    || "documents".equalsIgnoreCase(current)
                    || "assessments".equalsIgnoreCase(current)
                    || "users".equalsIgnoreCase(current)
                    || "tasks".equalsIgnoreCase(current)
                    || "forms".equalsIgnoreCase(current)
                    || "notes".equalsIgnoreCase(current)
                    || "organisations".equalsIgnoreCase(current)) {
                if (i + 1 < parts.length && !parts[i + 1].isBlank()) {
                    return parts[i + 1];
                }
            }
        }
        return null;
    }

    private String detailsWithContext(String details, String method, String path, int statusCode, long durationMs) {
        String safeDetails = details != null ? details : "";
        return GLOBAL_SOURCE_MARKER
                + ", method=" + method
                + ", path=" + path
                + ", status=" + statusCode
                + ", durationMs=" + durationMs
                + ", details=" + safeDetails;
    }

    private Map<String, Long> toCountMap(java.util.List<Object[]> rows) {
        if (rows == null || rows.isEmpty()) {
            return Map.of();
        }
        return rows.stream()
                .filter(row -> row != null && row.length >= 2)
                .collect(Collectors.toMap(
                        row -> row[0] == null ? "unknown" : String.valueOf(row[0]),
                        row -> ((Number) row[1]).longValue(),
                        Long::sum,
                        LinkedHashMap::new));
    }

    private Map<String, Long> limitTopN(Map<String, Long> source, Integer topN) {
        if (source == null || source.isEmpty() || topN == null || topN <= 0 || source.size() <= topN) {
            return source != null ? source : Map.of();
        }
        return source.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(topN)
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        Long::sum,
                        LinkedHashMap::new
                ));
    }
}
