package com.smart.therapy.flow.common.tenant;

import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Runs work per-tenant with TenantContext set. Skips inactive/archived/deleted/force-disabled tenants
 * and any schema that is missing. Always clears TenantContext after each tenant.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantExecutionService {

    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;

    public void runForEachActiveTenant(String jobName, Consumer<TenantDirectoryService.TenantInfo> tenantWork) {
        List<TenantDirectoryService.TenantInfo> tenants = new ArrayList<>(tenantDirectoryService.getBySchemaName().values());
        for (TenantDirectoryService.TenantInfo tenant : tenants) {
            if (tenant == null) continue;
            String schema = tenant.getSchemaName();
            if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) continue;
            if (!tenant.isActive() || tenant.isArchived() || tenant.isDeleted() || tenant.isForceDisabled()) {
                continue;
            }
            if (!tenantSchemaHealthService.schemaExists(schema)) {
                continue;
            }
            try {
                TenantContext.setSchemaName(schema);
                TenantContext.setOrganisationId(tenant.getOrganisationId());
                tenantWork.accept(tenant);
            } catch (Exception e) {
                log.warn("Tenant job '{}' failed for org {} ({}): {}", jobName, tenant.getOrganisationId(), schema, e.getMessage());
            } finally {
                TenantContext.clear();
            }
        }
    }
}
