package com.smart.therapy.flow.organisation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Audit trail for platform super-admin actions. All records in public.platform_audit_logs.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformAuditService {

    private final PlatformAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void log(Long authId, String action, String resourceType, String resourceId, String details) {
        try {
            repository.save(PlatformAuditLog.builder()
                    .authId(authId)
                    .action(action)
                    .resourceType(resourceType)
                    .resourceId(resourceId != null ? String.valueOf(resourceId) : null)
                    .details(details)
                    .createdAt(Instant.now())
                    .build());
        } catch (Exception e) {
            log.warn("Failed to write platform audit log: {}", e.getMessage());
        }
    }

    @Transactional
    public void logWithSnapshots(Long authId,
                                 String action,
                                 String resourceType,
                                 String resourceId,
                                 Object before,
                                 Object after,
                                 String details) {
        Map<String, Object> payload = new HashMap<>();
        if (before != null) {
            payload.put("before", before);
        }
        if (after != null) {
            payload.put("after", after);
        }
        if (details != null) {
            payload.put("details", details);
        }
        String jsonDetails = details;
        try {
            jsonDetails = objectMapper.writeValueAsString(payload);
        } catch (Exception ignored) {
        }
        log(authId, action, resourceType, resourceId, jsonDetails);
    }

    @Transactional
    public void logWithSnapshots(Long authId,
                                 String action,
                                 String resourceType,
                                 String resourceId,
                                 Object before,
                                 Object after) {
        logWithSnapshots(authId, action, resourceType, resourceId, before, after, null);
    }
}
