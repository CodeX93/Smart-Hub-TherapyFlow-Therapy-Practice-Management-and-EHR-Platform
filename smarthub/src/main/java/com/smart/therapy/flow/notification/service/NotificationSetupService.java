package com.smart.therapy.flow.notification.service;

import com.smart.therapy.flow.common.tenant.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class NotificationSetupService {

    private final NotificationService notificationService;

    /**
     * Sync required tenant notification defaults.
     * Current implementation relies on active tenant context; schema/org parameters
     * are validated for safety and future explicit routing support.
     */
    @Transactional
    public void syncTenantDefaults(String schema, Long orgId) {
        String currentSchema = TenantContext.getSchemaName();
        Long currentOrgId = TenantContext.getOrganisationId();
        if (schema != null && currentSchema != null && !schema.equalsIgnoreCase(currentSchema)) {
            throw new IllegalArgumentException("Schema mismatch for notification setup sync");
        }
        if (orgId != null && currentOrgId != null && !Objects.equals(orgId, currentOrgId)) {
            throw new IllegalArgumentException("Organisation mismatch for notification setup sync");
        }
        notificationService.syncTenantDefaults();
    }
}

