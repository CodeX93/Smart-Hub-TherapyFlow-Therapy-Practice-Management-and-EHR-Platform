package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.config.TenantMigrationDataSourceConfig;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantFlywayMigrator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Scheduled job: for each tenant schema, check presence of critical tables and that
 * Flyway version matches expected. Log to platform_audit_logs on drift.
 * Uses dedicated pool only.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TenantSchemaIntegrityChecker {

    private static final String[] CRITICAL_TABLES = {"users", "clients", "sessions"};
    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile("V(\\d+)__.*\\.sql");

    @Value("${tenant.integrity.expected-version:}")
    private String expectedVersion;
    @Value("${tenant.jobs.integrity-check.enabled:true}")
    private boolean integrityCheckEnabled;
    private volatile Boolean integrityCheckEnabledOverride;

    private final OrganisationRepository organisationRepository;
    private final PlatformAuditService platformAuditService;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    @jakarta.annotation.Resource(name = TenantMigrationDataSourceConfig.TENANT_MIGRATION_DATASOURCE)
    private DataSource tenantMigrationDataSource;

    @Scheduled(fixedDelayString = "${tenant.integrity-check.interval-ms:600000}", initialDelay = 120_000)
    public void checkAllTenants() {
        if (!isIntegrityCheckEnabled()) {
            return;
        }
        checkAllTenantsInternal();
    }

    public void runIntegrityCheckNow() {
        checkAllTenantsInternal();
    }

    public void setIntegrityCheckEnabledOverride(Boolean enabled) {
        this.integrityCheckEnabledOverride = enabled;
    }

    public Boolean getIntegrityCheckEnabledOverride() {
        return integrityCheckEnabledOverride;
    }

    public boolean isIntegrityCheckEnabled() {
        return integrityCheckEnabledOverride != null ? integrityCheckEnabledOverride : integrityCheckEnabled;
    }

    private void checkAllTenantsInternal() {
        List<Organisation> tenants = organisationRepository.findAll().stream()
                .filter(o -> o.getSchemaName() != null && !o.getSchemaName().isBlank() && !"public".equalsIgnoreCase(o.getSchemaName()))
                .toList();
        for (Organisation org : tenants) {
            try {
                checkOneTenant(org);
            } catch (Exception e) {
                log.warn("Integrity check failed for org {} ({}): {}", org.getId(), org.getSchemaName(), e.getMessage());
            }
        }
    }

    private void checkOneTenant(Organisation org) {
        String schema = org.getSchemaName();
        if (schema == null || schema.isBlank()) return;

        List<String> missingTables = checkCriticalTables(schema);
        String actualVersion = getFlywayVersion(schema);
        String expectedVersionForCheck = resolveExpectedVersion();

        boolean versionOk = expectedVersionForCheck.equals(actualVersion);
        if (!missingTables.isEmpty() || !versionOk) {
            String details = String.format("missingTables=%s, expectedVersion=%s, actualVersion=%s",
                    missingTables, expectedVersionForCheck, actualVersion != null ? actualVersion : "none");
            platformAuditService.log(null, "TENANT_SCHEMA_DRIFT", "Organisation", String.valueOf(org.getId()), details);
            log.warn("Tenant schema drift org {} ({}): {}", org.getId(), schema, details);
        }
    }

    private String resolveExpectedVersion() {
        if (expectedVersion != null && !expectedVersion.isBlank()) {
            return expectedVersion.trim();
        }
        return detectLatestTenantMigrationVersion();
    }

    private String detectLatestTenantMigrationVersion() {
        try {
            Resource[] resources = resourceResolver.getResources("classpath*:db/tenant_migration/V*__*.sql");
            int latestVersion = 0;
            for (Resource resource : resources) {
                String filename = resource.getFilename();
                if (filename == null) {
                    continue;
                }
                Matcher matcher = VERSIONED_MIGRATION_PATTERN.matcher(filename);
                if (matcher.matches()) {
                    int version = Integer.parseInt(matcher.group(1));
                    if (version > latestVersion) {
                        latestVersion = version;
                    }
                }
            }
            return String.valueOf(latestVersion);
        } catch (Exception ex) {
            log.warn("Failed to detect latest tenant migration version, falling back to 0: {}", ex.getMessage());
            return "0";
        }
    }

    private List<String> checkCriticalTables(String schemaName) {
        return java.util.Arrays.stream(CRITICAL_TABLES)
                .filter(t -> !tableExists(schemaName, t))
                .toList();
    }

    private boolean tableExists(String schemaName, String tableName) {
        String sql = "SELECT 1 FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (Connection conn = tenantMigrationDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, schemaName);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            log.debug("Table check failed for {}.{}: {}", schemaName, tableName, e.getMessage());
            return false;
        }
    }

    private String getFlywayVersion(String schemaName) {
        if (!schemaName.matches("[a-zA-Z0-9_]+")) return null;
        String sql = "SELECT version FROM " + schemaName + "." + TenantFlywayMigrator.TENANT_FLYWAY_HISTORY_TABLE +
                " ORDER BY installed_rank DESC LIMIT 1";
        try (Connection conn = tenantMigrationDataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getString(1) : null;
        } catch (SQLException e) {
            return null;
        }
    }
}
