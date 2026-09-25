package com.smart.therapy.flow.common.tenant;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.context.spi.CurrentTenantIdentifierResolver;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Resolves the current tenant schema name for Hibernate. When no tenant is set
 * (e.g. login, health), returns "public" so only public schema is used.
 * The resolved identifier is used by SchemaMultiTenantConnectionProvider to set
 * search_path on the connection (primary isolation). Do not use schema-qualified
 * table names (e.g. public.users) in native queries — use unqualified names only.
 */
@Component
public class CurrentTenantIdentifierResolverImpl implements CurrentTenantIdentifierResolver<String>, HibernatePropertiesCustomizer {

    @Override
    public String resolveCurrentTenantIdentifier() {
        String schema = TenantContext.getSchemaName();
        return (schema != null && !schema.isBlank()) ? schema : "public";
    }

    @Override
    public boolean validateExistingCurrentSessions() {
        return false;
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_IDENTIFIER_RESOLVER, this);
    }
}
