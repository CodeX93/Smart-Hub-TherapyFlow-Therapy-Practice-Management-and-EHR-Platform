package com.smart.therapy.flow.common.tenant;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a repository (or service) that accesses tenant-schema data only.
 * When schema is public (super-admin context), use of this component will throw.
 * Prevents accidentally querying tenant entities from platform endpoints.
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface TenantScoped {
}
