package com.smart.therapy.flow.common.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.exception.FlywayValidateException;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.boot.autoconfigure.flyway.FlywayConfigurationCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Platform Flyway runs on startup against {@code flyway_schema_history}.
 * Seed migrations may be made idempotent after first deploy; repair checksum drift instead of hard-failing.
 * Shared Azure DBs may have applied versions that are not present in this checkout — treat those as ignorable
 * (same intent as {@code spring.flyway.ignore-missing-migrations}).
 */
@Configuration
@Slf4j
public class PlatformFlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(
            Environment environment,
            ProductionMigrationSecurityScanner migrationSecurityScanner,
            ProductionSeedIdentityMigrationBlocker seedIdentityMigrationBlocker,
            ProductionSeedIdentityCleanup seedIdentityCleanup,
            @Value("${app.migration.remove-seeded-identities:false}") boolean removeSeededIdentities,
            @Value("${app.migration.production-migration-security:false}") boolean productionMigrationSecurity) {
        return flyway -> {
            boolean production = isProduction(environment);
            if (production && (!removeSeededIdentities || !productionMigrationSecurity)) {
                throw new IllegalStateException(
                        "Production Flyway requires seeded-identity cleanup and migration security guardrails");
            }
            if (production) {
                migrationSecurityScanner.assertProductionSafe(flyway);
            }
            validateOrRepairChecksumDrift(flyway);
            flyway.migrate();
            if (production) {
                seedIdentityCleanup.removeSeedIdentities();
                seedIdentityMigrationBlocker.removeBlocker();
            }
        };
    }

    @Bean
    @Profile({"prod", "production"})
    public FlywayConfigurationCustomizer productionSeedIdentityMigrationBlocker(
            ProductionSeedIdentityMigrationBlocker blocker) {
        return blocker::customize;
    }

    private static boolean isProduction(Environment environment) {
        return Arrays.stream(environment.getActiveProfiles())
                .anyMatch(profile -> "prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile));
    }

    private static void validateOrRepairChecksumDrift(Flyway flyway) {
        try {
            flyway.validate();
        } catch (FlywayValidateException ex) {
            if (isChecksumMismatch(ex)) {
                log.warn("Flyway checksum drift detected for applied migration(s); running repair. {}", ex.getMessage());
                flyway.repair();
                return;
            }
            if (isPendingMigration(ex)) {
                log.info("Flyway pending migration(s) detected; migrate will apply them next. {}", ex.getMessage());
                return;
            }
            if (isMissingLocalMigration(ex)) {
                log.warn(
                        "Flyway applied migration(s) missing from local classpath; continuing. {}",
                        ex.getMessage());
                return;
            }
            throw ex;
        }
    }

    private static boolean isChecksumMismatch(FlywayValidateException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("checksum mismatch");
    }

    private static boolean isPendingMigration(FlywayValidateException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("not applied to database");
    }

    private static boolean isMissingLocalMigration(FlywayValidateException ex) {
        String message = ex.getMessage();
        return message != null && message.contains("not resolved locally");
    }
}
