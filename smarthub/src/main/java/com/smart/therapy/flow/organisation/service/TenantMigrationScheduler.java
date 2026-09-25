package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Startup: validate Flyway config; optionally migrate all pending tenants when
 * {@code tenant.migration.run-on-startup=true} (intended for local dev only).
 * Background: migrate tenants in small batches to avoid pool starvation and long scheduler runs.
 * Batch size is clamped to a safe upper bound.
 */
@Component
@Order(100)
@RequiredArgsConstructor
@Slf4j
public class TenantMigrationScheduler implements ApplicationRunner {

    private static final Pattern VERSIONED_MIGRATION_PATTERN = Pattern.compile("V(\\d+)__.*\\.sql");

    private final TenantFlywayMigrator tenantFlywayMigrator;
    private final OrganisationRepository organisationRepository;
    private final PathMatchingResourcePatternResolver resourceResolver = new PathMatchingResourcePatternResolver();

    /** Batch size is clamped to a safe upper bound to avoid migration pool starvation. */
    @Value("${tenant.migration.batch-size:1}")
    private int batchSize;
    @Value("${tenant.jobs.migration.enabled:true}")
    private boolean migrationEnabled;
    @Value("${tenant.migration.run-on-startup:false}")
    private boolean runOnStartup;
    /** ClientHub cutover/sync boots should not fail on unrelated tenant Flyway conflicts. */
    @Value("${clienthub.migration.enabled:false}")
    private boolean clienthubMigrationEnabled;
    private volatile Boolean migrationEnabledOverride;

    @Override
    public void run(ApplicationArguments args) {
        if (clienthubMigrationEnabled) {
            log.info("Tenant migration: skipping startup Flyway validation because clienthub.migration.enabled=true");
            return;
        }
        tenantFlywayMigrator.validateConfiguration();
        if (runOnStartup && isMigrationEnabled()) {
            log.info("Tenant migration: run-on-startup enabled; migrating all pending tenants");
            runAllPendingOnStartup();
            return;
        }
        log.info("Tenant migration: startup validation only; background job will migrate queued tenants in batches of {}", clampBatchSize());
    }

    /**
     * Migrate a small batch of queued tenants. Runs after initial delay and then on fixed delay.
     * Batch size is clamped to avoid connection pool starvation.
     */
    @Scheduled(initialDelayString = "${tenant.migration.initial-delay-ms:60000}", fixedDelayString = "${tenant.migration.fixed-delay-ms:120000}")
    public void runBatch() {
        if (!isMigrationEnabled()) {
            return;
        }
        runBatchInternal();
    }

    public void runMigrationBatchNow() {
        runBatchInternal();
    }

    public void setMigrationEnabledOverride(Boolean enabled) {
        this.migrationEnabledOverride = enabled;
    }

    public Boolean getMigrationEnabledOverride() {
        return migrationEnabledOverride;
    }

    public boolean isMigrationEnabled() {
        return migrationEnabledOverride != null ? migrationEnabledOverride : migrationEnabled;
    }

    private void runAllPendingOnStartup() {
        int latestClasspathVersion = detectLatestTenantMigrationVersion();
        Set<Long> attemptedOrgIds = new HashSet<>();
        int migratedCount = 0;

        while (true) {
            List<Organisation> batch = organisationRepository
                    .findOrganisationsForMigration(latestClasspathVersion, PageRequest.of(0, clampBatchSize() * 10))
                    .getContent()
                    .stream()
                    .filter(org -> !attemptedOrgIds.contains(org.getId()))
                    .filter(org -> tenantFlywayMigrator.isBehindEffectiveTarget(org, latestClasspathVersion))
                    .limit(clampBatchSize())
                    .toList();
            if (batch.isEmpty()) {
                break;
            }
            log.info("Tenant migration startup: processing {} tenant(s) (classpath latest={})", batch.size(), latestClasspathVersion);
            for (Organisation org : batch) {
                attemptedOrgIds.add(org.getId());
                try {
                    tenantFlywayMigrator.migrateOneTenant(org);
                    migratedCount++;
                } catch (Exception e) {
                    log.error("Tenant migration failed for org {} (schema {}): {}", org.getId(), org.getSchemaName(), e.getMessage());
                }
            }
        }

        if (migratedCount == 0) {
            log.info("Tenant migration startup: no tenants needed migration (latestClasspathVersion={})", latestClasspathVersion);
        } else {
            log.info("Tenant migration startup: completed for {} tenant(s) (latestClasspathVersion={})", migratedCount, latestClasspathVersion);
        }
    }

    private void runBatchInternal() {
        int size = clampBatchSize();
        int latestClasspathVersion = detectLatestTenantMigrationVersion();
        List<Organisation> batch = organisationRepository
                .findOrganisationsForMigration(latestClasspathVersion, PageRequest.of(0, Math.max(size * 10, 20)))
                .getContent()
                .stream()
                .filter(org -> tenantFlywayMigrator.isBehindEffectiveTarget(org, latestClasspathVersion))
                .limit(size)
                .toList();
        if (batch.isEmpty()) {
            log.trace("Tenant migration batch: no tenants to migrate (latestClasspathVersion={})", latestClasspathVersion);
            return;
        }
        log.info("Tenant migration batch: processing {} tenant(s) (classpath latest={})", batch.size(), latestClasspathVersion);
        for (Organisation org : batch) {
            try {
                tenantFlywayMigrator.migrateOneTenant(org);
            } catch (Exception e) {
                log.error("Tenant migration failed for org {} (schema {}): {}", org.getId(), org.getSchemaName(), e.getMessage());
            }
        }
    }

    private int clampBatchSize() {
        if (batchSize <= 0) return 1;
        if (batchSize > 5) return 5;
        return batchSize;
    }

    private int detectLatestTenantMigrationVersion() {
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
            return latestVersion;
        } catch (Exception ex) {
            log.warn("Failed to detect latest tenant migration version; defaulting to 0: {}", ex.getMessage());
            return 0;
        }
    }
}
