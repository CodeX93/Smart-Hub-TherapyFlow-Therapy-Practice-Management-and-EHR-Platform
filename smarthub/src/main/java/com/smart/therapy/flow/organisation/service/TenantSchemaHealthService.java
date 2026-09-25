package com.smart.therapy.flow.organisation.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.smart.therapy.flow.common.config.TenantMigrationDataSourceConfig;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Checks whether a tenant schema exists in the database (information_schema.schemata).
 * Results are cached with a short TTL to avoid DB hit on every request; evicted on provisioning.
 */
@Service
@Slf4j
public class TenantSchemaHealthService {

    @Resource(name = TenantMigrationDataSourceConfig.TENANT_MIGRATION_DATASOURCE)
    private DataSource dataSource;

    /**
     * TTL for schema-exists cache in milliseconds. Default 5 minutes.
     */
    @Value("${tenant.schema-health.cache-ttl-ms:300000}")
    private long cacheTtlMs;

    /** schema name -> CachedResult(exists, expiresAt) */
    private final ConcurrentHashMap<String, CachedResult> cache = new ConcurrentHashMap<>();

    /**
     * Returns true if the schema exists in the database. Uses cache; cache entry expires after TTL.
     */
    public boolean schemaExists(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return true;
        }
        CachedResult cached = cache.get(schemaName);
        if (cached != null && System.currentTimeMillis() < cached.expiresAt) {
            return cached.exists;
        }
        boolean exists = checkSchemaExistsInDb(schemaName);
        cache.put(schemaName, new CachedResult(exists, System.currentTimeMillis() + cacheTtlMs));
        return exists;
    }

    /** Evict cache for a schema (e.g. after provisioning so next request sees the new schema). */
    public void evict(String schemaName) {
        if (schemaName != null) {
            cache.remove(schemaName);
        }
    }

    /** Evict entire cache. */
    public void evictAll() {
        cache.clear();
    }

    /**
     * Returns true when the named table exists in the tenant schema.
     */
    public boolean tableExists(String schemaName, String tableName) {
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return false;
        }
        if (tableName == null || tableName.isBlank()) {
            return false;
        }
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?")) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.warn("Table existence check failed for {}.{}: {}", schemaName, tableName, e.getMessage());
            return false;
        }
    }

    private boolean checkSchemaExistsInDb(String schemaName) {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT 1 FROM information_schema.schemata WHERE schema_name = ?")) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.warn("Schema existence check failed for {}: {}", schemaName, e.getMessage());
            return false;
        }
    }

    private static final class CachedResult {
        final boolean exists;
        final long expiresAt;

        CachedResult(boolean exists, long expiresAt) {
            this.exists = exists;
            this.expiresAt = expiresAt;
        }
    }
}
