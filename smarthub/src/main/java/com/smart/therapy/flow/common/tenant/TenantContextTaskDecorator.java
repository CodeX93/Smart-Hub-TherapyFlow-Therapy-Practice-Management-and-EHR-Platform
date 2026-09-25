package com.smart.therapy.flow.common.tenant;

import org.springframework.core.task.TaskDecorator;

/**
 * Propagates TenantContext to async tasks.
 * Ensures tenant-scoped repositories run with the correct schema when @Async is used.
 */
public class TenantContextTaskDecorator implements TaskDecorator {

    @Override
    public Runnable decorate(Runnable runnable) {
        String schema = TenantContext.getSchemaName();
        Long orgId = TenantContext.getOrganisationId();
        return () -> {
            try {
                TenantContext.setSchemaName(schema);
                TenantContext.setOrganisationId(orgId);
                runnable.run();
            } finally {
                TenantContext.clear();
            }
        };
    }
}
