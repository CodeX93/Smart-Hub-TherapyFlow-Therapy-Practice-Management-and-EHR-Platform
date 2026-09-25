package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.config.TenantMigrationDataSourceConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/**
 * Optional legacy helper: seeds empty tenant schemas from a known-good template before
 * incremental Flyway migrations run.
 * <p>
 * Prefer {@code V1__tenant_baseline.sql} via Flyway ({@code tenant.provisioning.clone-from-template=false}).
 * Cloning remains available for emergency fallback when the template schema exists.
 */
@Service
@Slf4j
public class TenantSchemaTemplateCloner {

    private static final String USERS_TABLE = "users";
    private static final String FLYWAY_HISTORY_TABLE = "tenant_flyway_schema_history";

    @Value("${tenant.provisioning.template-schema-name:tenant_northstar}")
    private String templateSchemaName;

    @Resource(name = TenantMigrationDataSourceConfig.TENANT_MIGRATION_DATASOURCE)
    private DataSource tenantMigrationDataSource;

    private final TenantSchemaHealthService tenantSchemaHealthService;

    public TenantSchemaTemplateCloner(TenantSchemaHealthService tenantSchemaHealthService) {
        this.tenantSchemaHealthService = tenantSchemaHealthService;
    }

    public void ensureBaselineSchema(String targetSchemaName) {
        if (!StringUtils.hasText(targetSchemaName)
                || "public".equalsIgnoreCase(targetSchemaName)
                || !StringUtils.hasText(templateSchemaName)
                || targetSchemaName.equalsIgnoreCase(templateSchemaName)) {
            return;
        }
        if (tenantSchemaHealthService.tableExists(targetSchemaName, USERS_TABLE)) {
            ensureFlywayHistoryFromTemplate(targetSchemaName);
            return;
        }
        if (!tenantSchemaHealthService.schemaExists(templateSchemaName)
                || !tenantSchemaHealthService.tableExists(templateSchemaName, USERS_TABLE)) {
            log.warn("Template schema {} is unavailable; skipping baseline clone for {}", templateSchemaName, targetSchemaName);
            return;
        }

        log.info("Cloning tenant baseline from {} into {}", templateSchemaName, targetSchemaName);
        try (Connection connection = tenantMigrationDataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                recreateTargetSchema(connection, targetSchemaName);
                cloneTables(connection, templateSchemaName, targetSchemaName);
                copyFlywayHistory(connection, templateSchemaName, targetSchemaName);
                connection.commit();
                tenantSchemaHealthService.evict(targetSchemaName);
                log.info("Tenant baseline clone complete for {}", targetSchemaName);
            } catch (SQLException ex) {
                connection.rollback();
                throw new IllegalStateException("Tenant baseline clone failed for " + targetSchemaName + ": " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException("Tenant baseline clone failed for " + targetSchemaName + ": " + ex.getMessage(), ex);
        }
    }

    /**
     * Repairs cloned tenants that have application tables but no Flyway history (legacy clone gap).
     */
    public void ensureFlywayHistoryFromTemplate(String targetSchemaName) {
        if (!StringUtils.hasText(targetSchemaName)
                || "public".equalsIgnoreCase(targetSchemaName)
                || !StringUtils.hasText(templateSchemaName)
                || targetSchemaName.equalsIgnoreCase(templateSchemaName)) {
            return;
        }
        if (tenantSchemaHealthService.tableExists(targetSchemaName, FLYWAY_HISTORY_TABLE)) {
            return;
        }
        if (!tenantSchemaHealthService.tableExists(targetSchemaName, USERS_TABLE)) {
            return;
        }
        if (!tenantSchemaHealthService.schemaExists(templateSchemaName)
                || !tenantSchemaHealthService.tableExists(templateSchemaName, FLYWAY_HISTORY_TABLE)) {
            log.warn("Template schema {} flyway history unavailable; cannot repair {}", templateSchemaName,
                    targetSchemaName);
            return;
        }
        log.info("Repairing missing {} in {} from template {}", FLYWAY_HISTORY_TABLE, targetSchemaName,
                templateSchemaName);
        try (Connection connection = tenantMigrationDataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                copyFlywayHistory(connection, templateSchemaName, targetSchemaName);
                connection.commit();
                tenantSchemaHealthService.evict(targetSchemaName);
            } catch (SQLException ex) {
                connection.rollback();
                throw new IllegalStateException(
                        "Flyway history repair failed for " + targetSchemaName + ": " + ex.getMessage(), ex);
            }
        } catch (SQLException ex) {
            throw new IllegalStateException(
                    "Flyway history repair failed for " + targetSchemaName + ": " + ex.getMessage(), ex);
        }
    }

    private void recreateTargetSchema(Connection connection, String targetSchemaName) throws SQLException {
        String safeTarget = sanitizeSchemaName(targetSchemaName);
        try (Statement statement = connection.createStatement()) {
            statement.execute("DROP SCHEMA IF EXISTS " + safeTarget + " CASCADE");
            statement.execute("CREATE SCHEMA " + safeTarget);
        }
    }

    private void cloneTables(Connection connection, String templateSchemaName, String targetSchemaName) throws SQLException {
        List<String> tables = listTables(connection, templateSchemaName);
        String safeTemplate = sanitizeSchemaName(templateSchemaName);
        String safeTarget = sanitizeSchemaName(targetSchemaName);
        for (String table : tables) {
            if (FLYWAY_HISTORY_TABLE.equalsIgnoreCase(table)) {
                continue;
            }
            String sql = "CREATE TABLE " + safeTarget + "." + quoteIdentifier(table)
                    + " (LIKE " + safeTemplate + "." + quoteIdentifier(table)
                    + " INCLUDING DEFAULTS INCLUDING GENERATED INCLUDING IDENTITY INCLUDING INDEXES INCLUDING STORAGE)";
            try (Statement statement = connection.createStatement()) {
                statement.execute(sql);
            }
        }
    }

    private void copyFlywayHistory(Connection connection, String templateSchemaName, String targetSchemaName)
            throws SQLException {
        if (!tableExists(connection, templateSchemaName, FLYWAY_HISTORY_TABLE)) {
            log.warn("Template schema {} has no {}; Flyway will create history on migrate", templateSchemaName,
                    FLYWAY_HISTORY_TABLE);
            return;
        }
        String safeTemplate = sanitizeSchemaName(templateSchemaName);
        String safeTarget = sanitizeSchemaName(targetSchemaName);
        try (Statement statement = connection.createStatement()) {
            statement.execute("CREATE TABLE " + safeTarget + "." + quoteIdentifier(FLYWAY_HISTORY_TABLE)
                    + " (LIKE " + safeTemplate + "." + quoteIdentifier(FLYWAY_HISTORY_TABLE) + " INCLUDING ALL)");
            statement.execute("INSERT INTO " + safeTarget + "." + quoteIdentifier(FLYWAY_HISTORY_TABLE)
                    + " SELECT * FROM " + safeTemplate + "." + quoteIdentifier(FLYWAY_HISTORY_TABLE));
        }
        log.info("Copied {} rows into {}.{} from template {}", countHistoryRows(connection, targetSchemaName),
                targetSchemaName, FLYWAY_HISTORY_TABLE, templateSchemaName);
    }

    private long countHistoryRows(Connection connection, String schemaName) throws SQLException {
        String safeSchema = sanitizeSchemaName(schemaName);
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(
                     "SELECT COUNT(*) FROM " + safeSchema + "." + quoteIdentifier(FLYWAY_HISTORY_TABLE))) {
            return rs.next() ? rs.getLong(1) : 0L;
        }
    }

    private boolean tableExists(Connection connection, String schemaName, String tableName) throws SQLException {
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT 1
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_name = ?
                """)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    private List<String> listTables(Connection connection, String schemaName) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = ?
                  AND table_type = 'BASE TABLE'
                ORDER BY table_name
                """)) {
            ps.setString(1, schemaName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tables.add(rs.getString(1));
                }
            }
        }
        return tables;
    }

    private static String sanitizeSchemaName(String schemaName) {
        if (!schemaName.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid schema name: " + schemaName);
        }
        return schemaName;
    }

    private static String quoteIdentifier(String identifier) {
        if (!identifier.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid identifier: " + identifier);
        }
        return identifier;
    }
}
