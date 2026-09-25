package com.smart.therapy.flow.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.CallbackException;
import org.hibernate.Interceptor;
import org.hibernate.type.Type;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Secondary guard: blocks use of tenant-scoped entities when schema is public.
 * Primary guard is TenantScopedGuardAspect (AOP on @TenantScoped), which runs before
 * repository methods; this interceptor runs on entity load/save only (native/projections
 * can hit DB before entity materialization, so rely on AOP as primary).
 */
@Component
@Slf4j
public class TenantScopeInterceptor implements Interceptor {

    private static final Set<Class<?>> TENANT_SCOPED_CLASSES = Set.of(
            com.smart.therapy.flow.auth.entity.User.class,
            com.smart.therapy.flow.client.entity.Client.class,
            com.smart.therapy.flow.session.entity.Session.class
    );

    @Override
    public boolean onLoad(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        checkTenantScope(entity);
        return false;
    }

    @Override
    public boolean onSave(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        checkTenantScope(entity);
        return false;
    }

    @Override
    public boolean onFlushDirty(Object entity, Object id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) throws CallbackException {
        checkTenantScope(entity);
        return false;
    }

    @Override
    public void onDelete(Object entity, Object id, Object[] state, String[] propertyNames, Type[] types) throws CallbackException {
        checkTenantScope(entity);
    }

    private void checkTenantScope(Object entity) {
        if (entity == null) return;
        Class<?> clazz = entity.getClass();
        for (Class<?> tenantClass : TENANT_SCOPED_CLASSES) {
            if (tenantClass.isInstance(entity)) {
                String schema = TenantContext.getSchemaName();
                if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
                    throw new IllegalStateException(
                            "Tenant-scoped entity " + clazz.getSimpleName() + " cannot be used when schema is public. Use a tenant context or super-admin public-only APIs.");
                }
                return;
            }
        }
    }
}
