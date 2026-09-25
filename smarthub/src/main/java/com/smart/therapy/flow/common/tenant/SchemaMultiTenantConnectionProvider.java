package com.smart.therapy.flow.common.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.jdbc.connections.spi.MultiTenantConnectionProvider;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * For schema-per-tenant: sets search_path on the connection so that
 * unqualified table names resolve only to the tenant schema (no public in path for tenants).
 * This is the primary isolation mechanism; do not use schema-qualified table names (e.g. public.users) in native queries.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SchemaMultiTenantConnectionProvider implements MultiTenantConnectionProvider<String>, HibernatePropertiesCustomizer {

    private final DataSource dataSource;

    @Override
    public Connection getAnyConnection() throws SQLException {
        return dataSource.getConnection();
    }

    @Override
    public void releaseAnyConnection(Connection connection) throws SQLException {
        connection.close();
    }

    @Override
    public Connection getConnection(String tenantIdentifier) throws SQLException {
        Connection connection = getAnyConnection();
        try {
            String schema = (tenantIdentifier != null && !tenantIdentifier.isBlank()) ? tenantIdentifier : "public";
            if ("public".equalsIgnoreCase(schema)) {
                // Critical: lock to public only — no tenant schema. Prevents leaking tenant_XX from pooled connection.
                connection.createStatement().execute("SET search_path TO public");
                log.trace("Set search_path TO public (locked)");
            } else {
                // Tenant only — no public in path so unqualified names never resolve to public (cross-tenant safety).
                connection.createStatement().execute("SET search_path TO " + schema);
                log.trace("Set search_path to {} (tenant only)", schema);
            }
        } catch (SQLException e) {
            connection.close();
            throw e;
        }
        return connection;
    }

    @Override
    public void releaseConnection(String tenantIdentifier, Connection connection) throws SQLException {
        try {
            // Always leave connection in safe state: public only. Never leave tenant schema set for next consumer.
            connection.createStatement().execute("SET search_path TO public");
        } catch (SQLException e) {
            log.debug("Set search_path on release failed (ignored): {}", e.getMessage());
        }
        releaseAnyConnection(connection);
    }

    @Override
    public boolean supportsAggressiveRelease() {
        return false;
    }

    @Override
    public boolean isUnwrappableAs(Class<?> unwrapType) {
        return MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType);
    }

    @Override
    public <T> T unwrap(Class<T> unwrapType) {
        if (MultiTenantConnectionProvider.class.isAssignableFrom(unwrapType)) {
            return unwrapType.cast(this);
        }
        throw new IllegalArgumentException("Cannot unwrap to " + unwrapType);
    }

    @Override
    public void customize(Map<String, Object> hibernateProperties) {
        hibernateProperties.put(AvailableSettings.MULTI_TENANT_CONNECTION_PROVIDER, this);
    }
}
