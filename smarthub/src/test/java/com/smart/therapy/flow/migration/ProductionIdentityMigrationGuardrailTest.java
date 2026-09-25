package com.smart.therapy.flow.migration;

import com.smart.therapy.flow.common.config.ProductionMigrationSecurityScanner;
import com.smart.therapy.flow.common.config.ProductionSeedIdentityCleanup;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class ProductionIdentityMigrationGuardrailTest {

    @Test
    void productionProfileEnablesFailClosedSeedIdentityCleanup() throws Exception {
        String application = Files.readString(Path.of("src/main/resources/application.yml"));

        assertThat(application)
                .contains("remove-seeded-identities: true")
                .contains("production-migration-security: true");
    }

    @Test
    void productionMigrationRecordsSeedIdentityCleanupEvidence() throws Exception {
        Path migration = Path.of("src/main/resources/db/migration/V93__production_identity_cleanup_evidence.sql");

        assertThat(migration).isRegularFile();
        assertThat(Files.readString(migration))
                .startsWith("-- PLATFORM")
                .contains("platform_migration_evidence")
                .contains("production_seed_identity_cleanup");
    }

    @Test
    void productionFlywayStrategyScansBeforeMigratingAndCleansAfterMigrating() throws Exception {
        String flywayConfig = Files.readString(Path.of(
                "src/main/java/com/smart/therapy/flow/common/config/PlatformFlywayConfig.java"));

        assertThat(flywayConfig)
                .contains("ProductionMigrationSecurityScanner")
                .contains("ProductionSeedIdentityMigrationBlocker")
                .contains("ProductionSeedIdentityCleanup")
                .contains("assertProductionSafe")
                .contains("removeSeedIdentities")
                .contains("removeBlocker");
    }

    @Test
    void productionMigrationBlockerKeepsHistoricalSeedRowsInactiveAndPasswordless() throws Exception {
        String blocker = Files.readString(Path.of(
                "src/main/java/com/smart/therapy/flow/common/config/ProductionSeedIdentityMigrationBlocker.java"));

        assertThat(blocker)
                .contains("BEFORE INSERT OR UPDATE")
                .contains("NEW.is_active := false")
                .contains("NEW.password_hash := NULL")
                .contains("@therapyflowseed.com");
    }

    @Test
    void productionMigrationScannerRejectsFixedHashesAndKnownSeedIdentifiers() {
        ProductionMigrationSecurityScanner scanner = new ProductionMigrationSecurityScanner();

        assertThat(scanner.scan(
                "V94__unsafe.sql",
                "INSERT INTO auth_identities(password_hash, login_identifier) "
                        + "VALUES ('$2a$10$zHJZ0RLC9E/t9fSc5g4KYeZ1iLAMhJbUv519dJvcaGx4eIzaB4oZW', "
                        + "'superadmin@therapyflow.com');"))
                .hasSize(2);
    }

    @Test
    void historicalSeedMigrationsAreExplicitlyQuarantinedAndTheirInventoryIsComplete() {
        ProductionMigrationSecurityScanner scanner = new ProductionMigrationSecurityScanner();

        assertThat(scanner.isHistoricalSeedMigration("V3__platform_seed_auth.sql")).isTrue();
        assertThat(scanner.isHistoricalSeedMigration("V36__seed_two_tenants_default_users.sql")).isTrue();
        assertThat(scanner.isHistoricalSeedMigration("V75__seed_additional_platform_super_admins.sql")).isTrue();
        assertThat(scanner.isHistoricalSeedMigration("V76__seed_amjad_aqeel_platform_super_admins.sql")).isTrue();
        assertThat(scanner.knownSeedIdentifiers())
                .containsAll(ProductionSeedIdentityCleanup.seededIdentifiers());
    }

    @Test
    void classpathProductionMigrationsPassTheSecurityScanner() {
        ProductionMigrationSecurityScanner scanner = new ProductionMigrationSecurityScanner();
        Flyway flyway = Flyway.configure()
                .locations("classpath:db/migration")
                .load();

        scanner.assertProductionSafe(flyway);
    }
}
