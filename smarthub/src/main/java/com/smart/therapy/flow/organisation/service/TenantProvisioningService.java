package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.config.TenantMigrationDataSourceConfig;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.TenantSchemaVersion;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.TenantSchemaVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Creates a new tenant schema and runs Flyway tenant migrations. Provisioning only
 * creates the schema (CREATE SCHEMA); Flyway ({@code db/tenant_migration}, starting at
 * {@code V1__tenant_baseline.sql}) is the source of truth for tenant DDL.
 * Optional legacy cloning from {@code tenant_northstar} is controlled by
 * {@code tenant.provisioning.clone-from-template} (default false).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantProvisioningService {

    private final OrganisationRepository organisationRepository;
    private final TenantSchemaVersionRepository tenantSchemaVersionRepository;
    private final TenantFlywayMigrator tenantFlywayMigrator;

    @jakarta.annotation.Resource(name = TenantMigrationDataSourceConfig.TENANT_MIGRATION_DATASOURCE)
    private DataSource tenantMigrationDataSource;

    /**
     * Queue tenant provisioning for background migration.
     */
    public void queueTenantProvisioning(Long organisationId) {
        Organisation org = requireProvisionableOrganisation(organisationId);
        TenantSchemaVersion record = tenantSchemaVersionRepository.findByOrganisationId(organisationId).orElse(null);
        if (record == null) {
            record = TenantSchemaVersion.builder()
                    .organisationId(organisationId)
                    .version("0")
                    .status(TenantFlywayMigrator.STATUS_PENDING)
                    .errorMessage(null)
                    .build();
        } else {
            record.setStatus(TenantFlywayMigrator.STATUS_PENDING);
            record.setErrorMessage(null);
            record.setMigratedAt(null);
        }
        tenantSchemaVersionRepository.save(record);
        log.info("Queued tenant provisioning for org {} (schema {})", organisationId, org.getSchemaName());
    }

    /**
     * Create schema for the organisation and run Flyway tenant migrations.
     * Uses dedicated pool; holds pg_advisory_lock(organisationId) for the duration. Idempotent.
     */
    public TenantFlywayMigrator.TenantMigrationResult provisionTenantSchema(Long organisationId) {
        Organisation org = requireProvisionableOrganisation(organisationId);
        String schemaName = org.getSchemaName();
        String safe = sanitizeSchemaName(schemaName);
        try (Connection connection = tenantMigrationDataSource.getConnection()) {
            try {
                try (Statement lockStmt = connection.createStatement()) {
                    lockStmt.execute("SELECT pg_advisory_lock(" + organisationId + ")");
                }
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("CREATE SCHEMA IF NOT EXISTS " + safe);
                }
                return tenantFlywayMigrator.migrateOneTenant(org);
            } finally {
                try (Statement unlockStmt = connection.createStatement()) {
                    unlockStmt.execute("SELECT pg_advisory_unlock(" + organisationId + ")");
                } catch (SQLException e) {
                    log.warn("Advisory unlock failed for org {}: {}", organisationId, e.getMessage());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Tenant provisioning failed for " + organisationId + ": " + e.getMessage(), e);
        }
    }

    /**
     * Full reprovision: reset migration queue state, clone baseline if needed, run Flyway, then seed defaults
     * (staff profiles, system options, library, billing services) — same path as the background scheduler.
     */
    public TenantFlywayMigrator.TenantMigrationResult reprovisionTenant(Long organisationId) {
        queueTenantProvisioning(organisationId);
        return provisionTenantSchema(organisationId);
    }

    private Organisation requireProvisionableOrganisation(Long organisationId) {
        Organisation org = organisationRepository.findById(organisationId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + organisationId));
        String schemaName = org.getSchemaName();
        if (schemaName == null || schemaName.isBlank()) {
            throw new IllegalStateException("Organisation has no schema_name: " + organisationId);
        }
        if ("public".equalsIgnoreCase(schemaName)) {
            throw new IllegalArgumentException("Cannot provision public schema as tenant");
        }
        return org;
    }

    private static String sanitizeSchemaName(String name) {
        if (!name.matches("[a-zA-Z0-9_]+")) {
            throw new IllegalArgumentException("Invalid schema name (alphanumeric and underscore only): " + name);
        }
        return name;
    }
}
