package com.smart.therapy.flow.common.tenant;

import lombok.experimental.UtilityClass;

/**
 * Holds the current tenant schema name for the request thread.
 * Set by TenantFilter from subdomain (or X-Tenant-Schema header). Used by
 * Hibernate's CurrentTenantIdentifierResolver and MultiTenantConnectionProvider.
 */
@UtilityClass
public class TenantContext {

    private static final ThreadLocal<String> SCHEMA_NAME = new ThreadLocal<>();
    private static final ThreadLocal<Long> ORGANISATION_ID = new ThreadLocal<>();

    public static void setSchemaName(String schemaName) {
        SCHEMA_NAME.set(schemaName);
    }

    public static String getSchemaName() {
        return SCHEMA_NAME.get();
    }

    public static void setOrganisationId(Long organisationId) {
        ORGANISATION_ID.set(organisationId);
    }

    public static Long getOrganisationId() {
        return ORGANISATION_ID.get();
    }

    public static void clear() {
        SCHEMA_NAME.remove();
        ORGANISATION_ID.remove();
    }
}
