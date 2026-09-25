package com.smart.therapy.flow.common.tenant;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Primary guard for tenant isolation: when TenantContext is public (or null), forbids
 * invocation of @TenantScoped beans (e.g. repositories). Runs at repository method level
 * before any DB call. TenantScopeInterceptor is a secondary safeguard on entity load/save
 * only; native queries, projections and derived queries are protected by this AOP.
 */
@Aspect
@Component
@Order(1)
@Slf4j
public class TenantScopedGuardAspect {

    @Around("@within(com.smart.therapy.flow.common.tenant.TenantScoped)")
    public Object guardTenantScoped(ProceedingJoinPoint pjp) throws Throwable {
        String schema = TenantContext.getSchemaName();
        boolean isPublic = schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema);
        if (isPublic) {
            String target = pjp.getTarget().getClass().getSimpleName();
            log.error("TenantScoped component {} invoked with schema=public; refusing to prevent tenant data leak", target);
            throw new IllegalStateException(
                    "Tenant-scoped component cannot be used when schema is public. Use platform endpoints only.");
        }
        return pjp.proceed();
    }
}
