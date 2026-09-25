package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.config.TenantMigrationDataSourceConfig;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.entity.TenantSchemaVersion;
import com.smart.therapy.flow.organisation.repository.TenantSchemaVersionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Runs Flyway for tenant schemas. Used by provisioning (single tenant) and by
 * {@link TenantMigrationScheduler} in batches. Does not run on startup — startup only validates.
 * Uses dedicated pool only. Tracks status (SUCCESS/FAILED/IN_PROGRESS) and error_message.
 * <p>
 * New tenants are built from {@code V1__tenant_baseline.sql} + incremental migrations.
 * Cloning {@code tenant_northstar} is optional legacy behavior (off by default).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantFlywayMigrator {

    public static final String TENANT_MIGRATION_LOCATIONS = "classpath:db/tenant_migration";
    public static final String TENANT_FLYWAY_HISTORY_TABLE = "tenant_flyway_schema_history";
    public static final String STATUS_PENDING = "PENDING";
    public static final String STATUS_SUCCESS = "SUCCESS";
    public static final String STATUS_FAILED = "FAILED";
    public static final String STATUS_IN_PROGRESS = "IN_PROGRESS";
    public static final String STATUS_SKIPPED = "SKIPPED";

    private final TenantSchemaVersionRepository tenantSchemaVersionRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final TenantSchemaTemplateCloner tenantSchemaTemplateCloner;
    private final TenantStaffProfileBootstrapService tenantStaffProfileBootstrapService;
    private final TenantSystemOptionSeedService tenantSystemOptionSeedService;
    private final TenantLibrarySeedService tenantLibrarySeedService;
    private final TenantBillingServiceSeedService tenantBillingServiceSeedService;
    private final TenantNotificationSeedService tenantNotificationSeedService;
    private final TenantClinicalTemplateSeedService tenantClinicalTemplateSeedService;
    private final TenantPracticeConfigurationSeedService tenantPracticeConfigurationSeedService;

    @jakarta.annotation.Resource(name = TenantMigrationDataSourceConfig.TENANT_MIGRATION_DATASOURCE)
    private DataSource tenantMigrationDataSource;

    @Value("${tenant.provisioning.clone-from-template:false}")
    private boolean cloneFromTemplate;

    /**
     * First version that requires pilot allow-list for <em>existing</em> tenants.
     * New tenants (schema version 0 / first provision) always migrate to latest.
     */
    @Value("${tenant.migration.gated-from-version:56}")
    private int gatedFromVersion;

    @Value("${tenant.migration.gate-allow-slugs:}")
    private String gateAllowSlugs;

    @Value("${tenant.migration.gate-allow-all:true}")
    private boolean gateAllowAll;

    /**
     * Validate Flyway config and tenant_migration location only. Does not migrate.
     * Call at startup to fail fast if config is broken.
     */
    public void validateConfiguration() {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(tenantMigrationDataSource)
                    .table(TENANT_FLYWAY_HISTORY_TABLE)
                    .locations(TENANT_MIGRATION_LOCATIONS)
                    .validateOnMigrate(false)
                    .ignoreMigrationPatterns("*:missing")
                    .load();
            // This only verifies Flyway wiring and migration discovery.
            // Avoid startup hard-fail due checksum drift in the public flyway_schema_history table.
            flyway.info();
            log.debug("Tenant Flyway configuration validated");
        } catch (Exception e) {
            log.error("Tenant Flyway validation failed: {}", e.getMessage());
            throw new IllegalStateException("Tenant Flyway validation failed", e);
        }
    }

    /**
     * Run Flyway for a single tenant schema and update tenant_schema_versions (status + error_message).
     */
    public TenantMigrationResult migrateOneTenant(Organisation org) {
        String schemaName = org.getSchemaName();
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return new TenantMigrationResult("0", STATUS_SKIPPED, null, new TenantSeedResult(0, 0, 0, 0, 0, 0));
        }
        Long orgId = org.getId();
        setMigrationStatus(orgId, STATUS_IN_PROGRESS, null);
        try {
            if (cloneFromTemplate) {
                log.info("Cloning tenant baseline template into {} (clone-from-template=true)", schemaName);
                tenantSchemaTemplateCloner.ensureBaselineSchema(schemaName);
            } else {
                log.debug("Skipping template clone for {}; relying on V1 tenant baseline migration", schemaName);
            }
            var configure = Flyway.configure()
                    .dataSource(tenantMigrationDataSource)
                    .schemas(schemaName)
                    .defaultSchema(schemaName)
                    .createSchemas(true)
                    .table(TENANT_FLYWAY_HISTORY_TABLE)
                    .baselineOnMigrate(true)
                    .baselineVersion("0")
                    .outOfOrder(true)
                    .locations(TENANT_MIGRATION_LOCATIONS)
                    .validateOnMigrate(false)
                    .ignoreMigrationPatterns("*:missing");

            int priorVersion = priorVersionNumber(orgId);
            String targetCap = resolveMigrationTargetCap(org, priorVersion);
            if (targetCap != null) {
                log.info("Tenant schema {}: applying migrations with target={} (PHI pilot gate; slug={})",
                        schemaName, targetCap, org.getSlug());
                configure.target(targetCap);
            } else {
                log.info("Tenant schema {}: applying migrations to latest (slug={}, pilot/new-tenant)",
                        schemaName, org.getSlug());
            }

            Flyway flyway = configure.load();
            String priorStatus = tenantSchemaVersionRepository.findByOrganisationId(orgId)
                    .map(TenantSchemaVersion::getStatus)
                    .orElse(STATUS_PENDING);
            // Always repair so edited baselines (e.g. V1 transcript tables) do not fail on checksum drift.
            // Also clears failed migration markers so fixed SQL can be re-applied.
            flyway.repair();
            if (STATUS_FAILED.equalsIgnoreCase(priorStatus)) {
                log.info("Repaired Flyway history for previously failed tenant schema {}", schemaName);
            }
            var result = flyway.migrate();
            if (result != null && result.migrationsExecuted > 0) {
                log.info("Tenant schema {}: {} migration(s) executed", schemaName, result.migrationsExecuted);
            }
            String version = flyway.info().current() != null ? flyway.info().current().getVersion().getVersion() : "0";
            setSchemaVersionSuccess(orgId, version);
            TenantSeedResult seeds = seedTenantDefaults(org);
            return new TenantMigrationResult(version, STATUS_SUCCESS, null, seeds);
        } catch (Exception e) {
            log.error("Tenant migration failed for org {} (schema {}): {}", orgId, schemaName, e.getMessage(), e);
            setMigrationStatus(orgId, STATUS_FAILED, e.getMessage());
            throw e;
        }
    }

    /**
     * Seeds system options, library, billing services, notification action metadata, clinical templates, and staff profiles.
     * Safe to call after manual schema clone when Flyway post-migration hooks were skipped.
     * Staff profiles are seeded first so library entry ownership can resolve a tenant user.
     */
    public TenantSeedResult seedTenantDefaults(Organisation org) {
        if (org == null) return new TenantSeedResult(0, 0, 0, 0, 0, 0);
        Long orgId = org.getId();
        String schemaName = org.getSchemaName();
        if (orgId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return new TenantSeedResult(0, 0, 0, 0, 0, 0);
        }
        try {
            tenantPracticeConfigurationSeedService.seedDefaults(orgId, schemaName, org.getTimezone(), org.getName());
        } catch (Exception ex) {
            log.warn("Tenant practice configuration seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        int staff = 0;
        int options = 0;
        int library = 0;
        int billing = 0;
        int notifications = 0;
        int clinicalTemplates = 0;
        try {
            staff = tenantStaffProfileBootstrapService.ensureStaffProfiles(orgId, schemaName, org.getTimezone());
        } catch (Exception ex) {
            log.warn("Tenant staff profile bootstrap failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        try {
            options = tenantSystemOptionSeedService.seedDefaults(orgId, schemaName);
        } catch (Exception ex) {
            log.warn("Tenant system option seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        try {
            library = tenantLibrarySeedService.seedDefaults(orgId, schemaName);
        } catch (Exception ex) {
            log.warn("Tenant library seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        try {
            billing = tenantBillingServiceSeedService.seedDefaults(orgId, schemaName);
        } catch (Exception ex) {
            log.warn("Tenant billing service seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        try {
            notifications = tenantNotificationSeedService.seedDefaults(orgId, schemaName);
        } catch (Exception ex) {
            log.warn("Tenant notification seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        try {
            clinicalTemplates = tenantClinicalTemplateSeedService.seedDefaults(orgId, schemaName);
        } catch (Exception ex) {
            log.warn("Tenant clinical template seed failed for org {} (schema {}): {}", orgId, schemaName, ex.getMessage());
        }
        tenantSchemaHealthService.evict(schemaName);
        return new TenantSeedResult(staff, options, library, billing, notifications, clinicalTemplates);
    }

    public record TenantSeedResult(
            int staffProfiles,
            int systemOptions,
            int libraryRecords,
            int billingServices,
            int notificationActionMetadata,
            int clinicalTemplateRecords
    ) {}

    public record TenantMigrationResult(String version, String migrationStatus, String errorMessage, TenantSeedResult seeds) {}

    /**
     * Whether this org may migrate past {@link #gatedFromVersion} (PHI blind indexes and later).
     * Pilot allow-listed slugs, full rollout flag, and brand-new tenants (version 0) qualify.
     */
    public boolean mayMigratePastGate(Organisation org, int currentVersion) {
        if (gateAllowAll || gatedFromVersion <= 0) {
            return true;
        }
        if (currentVersion <= 0) {
            return true; // new tenant / first provision
        }
        return isSlugAllowListed(org);
    }

    /**
     * Effective migration target for scheduler candidate filtering.
     * Existing non-pilot tenants cap at gatedFromVersion - 1 (e.g. 55).
     */
    public int effectiveMigrationTargetVersion(Organisation org, int latestClasspathVersion) {
        int current = priorVersionNumber(org.getId());
        if (mayMigratePastGate(org, current)) {
            return latestClasspathVersion;
        }
        int cap = Math.max(0, gatedFromVersion - 1);
        return Math.min(latestClasspathVersion, cap);
    }

    /** True when tenant schema version is below the gated effective target. */
    public boolean isBehindEffectiveTarget(Organisation org, int latestClasspathVersion) {
        int current = priorVersionNumber(org.getId());
        return current < effectiveMigrationTargetVersion(org, latestClasspathVersion);
    }

    /**
     * @return Flyway target version string to cap at, or {@code null} for latest
     */
    private String resolveMigrationTargetCap(Organisation org, int currentVersion) {
        if (mayMigratePastGate(org, currentVersion)) {
            return null;
        }
        int cap = Math.max(0, gatedFromVersion - 1);
        if (currentVersion >= cap) {
            // Already at/above allowed cap — Flyway target keeps them from applying gated versions
            return String.valueOf(cap);
        }
        return String.valueOf(cap);
    }

    private boolean isSlugAllowListed(Organisation org) {
        if (org == null || org.getSlug() == null || org.getSlug().isBlank()) {
            return false;
        }
        String slug = org.getSlug().trim().toLowerCase(Locale.ROOT);
        return allowListedSlugs().contains(slug);
    }

    private Set<String> allowListedSlugs() {
        if (gateAllowSlugs == null || gateAllowSlugs.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(gateAllowSlugs.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private int priorVersionNumber(Long organisationId) {
        return tenantSchemaVersionRepository.findByOrganisationId(organisationId)
                .map(TenantSchemaVersion::getVersion)
                .map(this::parseVersionNumber)
                .orElse(0);
    }

    private int parseVersionNumber(String version) {
        if (version == null || version.isBlank()) {
            return 0;
        }
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("^(\\d+)").matcher(version.trim());
        if (!m.find()) {
            return 0;
        }
        try {
            return Integer.parseInt(m.group(1));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private void setMigrationStatus(Long organisationId, String status, String errorMessage) {
        TenantSchemaVersion existing = tenantSchemaVersionRepository.findByOrganisationId(organisationId).orElse(null);
        if (existing != null) {
            existing.setStatus(status);
            existing.setErrorMessage(errorMessage);
            tenantSchemaVersionRepository.save(existing);
        } else {
            tenantSchemaVersionRepository.save(TenantSchemaVersion.builder()
                    .organisationId(organisationId)
                    .version("0")
                    .status(status)
                    .errorMessage(errorMessage)
                    .build());
        }
    }

    private void setSchemaVersionSuccess(Long organisationId, String version) {
        TenantSchemaVersion existing = tenantSchemaVersionRepository.findByOrganisationId(organisationId).orElse(null);
        if (existing != null) {
            existing.setVersion(version);
            existing.setMigratedAt(Instant.now());
            existing.setStatus(STATUS_SUCCESS);
            existing.setErrorMessage(null);
            tenantSchemaVersionRepository.save(existing);
        } else {
            tenantSchemaVersionRepository.save(TenantSchemaVersion.builder()
                    .organisationId(organisationId)
                    .version(version)
                    .migratedAt(Instant.now())
                    .status(STATUS_SUCCESS)
                    .build());
        }
    }
}
