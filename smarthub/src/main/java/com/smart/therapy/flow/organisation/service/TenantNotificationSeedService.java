package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seeds tenant notification setup defaults (action metadata) after provisioning.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantNotificationSeedService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final NotificationService notificationService;

    public int seedDefaults(Long organisationId, String schemaName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }
        Integer created = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            var result = notificationService.seedDefaultActionMetadata(false);
            return result != null ? result.getCreated() : 0;
        });
        if (created != null && created > 0) {
            log.info("Seeded {} notification action metadata row(s) for org {} (schema {})",
                    created, organisationId, schemaName);
        }
        return created != null ? created : 0;
    }
}
