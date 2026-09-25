package com.smart.therapy.flow.common.tenant;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

/**
 * Reusable tenant-scoped execution helper.
 * Ensures TenantContext is set before transaction start so Hibernate resolves tenant schema correctly.
 */
@Component
@RequiredArgsConstructor
public class TenantTransactionExecutor {

    private final PlatformTransactionManager transactionManager;

    @PersistenceContext
    private EntityManager entityManager;

    @Resource(name = "tenantIsolationExecutor")
    private Executor tenantIsolationExecutor;

    public <T> T executeReadOnly(Long organisationId, String schemaName, Supplier<T> work) {
        return execute(organisationId, schemaName, true, work);
    }

    public <T> T executeWrite(Long organisationId, String schemaName, Supplier<T> work) {
        return execute(organisationId, schemaName, false, work);
    }

    public void runReadOnly(Long organisationId, String schemaName, Runnable work) {
        executeReadOnly(organisationId, schemaName, () -> {
            work.run();
            return null;
        });
    }

    public void runWrite(Long organisationId, String schemaName, Runnable work) {
        executeWrite(organisationId, schemaName, () -> {
            work.run();
            return null;
        });
    }

    /**
     * Runs tenant work on a dedicated thread so callers on platform-scoped requests (e.g. Stripe webhooks)
     * do not reuse an open-session-in-view Hibernate session bound to the public schema.
     */
    public void runWriteIsolated(Long organisationId, String schemaName, Runnable work) {
        executeWriteIsolated(organisationId, schemaName, () -> {
            work.run();
            return null;
        });
    }

    public <T> T executeReadOnlyIsolated(Long organisationId, String schemaName, Supplier<T> work) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> executeReadOnly(organisationId, schemaName, work),
                    tenantIsolationExecutor
            ).join();
        } catch (CompletionException ex) {
            throw rethrowRuntime(ex.getCause());
        }
    }

    public <T> T executeWriteIsolated(Long organisationId, String schemaName, Supplier<T> work) {
        try {
            return CompletableFuture.supplyAsync(
                    () -> executeWrite(organisationId, schemaName, work),
                    tenantIsolationExecutor
            ).join();
        } catch (CompletionException ex) {
            throw rethrowRuntime(ex.getCause());
        }
    }

    private static RuntimeException rethrowRuntime(Throwable cause) {
        if (cause instanceof RuntimeException runtime) {
            return runtime;
        }
        return new IllegalStateException(cause);
    }

    private <T> T execute(Long organisationId, String schemaName, boolean readOnly, Supplier<T> work) {
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            throw new IllegalArgumentException("Tenant schema is required for tenant-scoped execution");
        }

        String previousSchema = TenantContext.getSchemaName();
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            // Drop any OSIV session bound to the previous tenant (e.g. public schema on webhook endpoints).
            entityManager.clear();
            TenantContext.setSchemaName(schemaName);
            TenantContext.setOrganisationId(organisationId);

            TransactionTemplate tx = new TransactionTemplate(transactionManager);
            tx.setReadOnly(readOnly);
            tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
            return tx.execute(status -> work.get());
        } finally {
            TenantContext.clear();
            if (previousSchema != null && !previousSchema.isBlank()) {
                TenantContext.setSchemaName(previousSchema);
            }
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            }
        }
    }
}
