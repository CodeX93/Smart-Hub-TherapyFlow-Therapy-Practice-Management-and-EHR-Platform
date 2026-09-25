package com.smart.therapy.flow.tools;

import org.flywaydb.core.Flyway;

/**
 * One-off: apply tenant Flyway migrations to MindCare Wellness Hospital (tenant_53) only.
 * Run: see docs / shell snippet — not part of app startup.
 */
public final class ApplyMindcarePhiBlindIndexMigration {

    private ApplyMindcarePhiBlindIndexMigration() {
    }

    public static void main(String[] args) {
        String url = env("DB_URL",
                "jdbc:postgresql://client-hub.postgres.database.azure.com:5432/therapyflow?sslmode=require");
        String user = env("DB_USERNAME", "clientHub");
        String pass = env("DB_PASSWORD", "npg_aV4PkYKLRzc8");
        String schema = args.length > 0 ? args[0] : "tenant_53";

        Flyway flyway = Flyway.configure()
                .dataSource(url, user, pass)
                .schemas(schema)
                .defaultSchema(schema)
                .table("tenant_flyway_schema_history")
                .locations("classpath:db/tenant_migration")
                .baselineOnMigrate(true)
                .baselineVersion("0")
                .outOfOrder(true)
                .validateOnMigrate(false)
                .ignoreMigrationPatterns("*:missing")
                .load();
        flyway.repair();
        var result = flyway.migrate();
        System.out.println("schema=" + schema);
        System.out.println("migrationsExecuted=" + result.migrationsExecuted);
        System.out.println("current=" + (flyway.info().current() != null
                ? flyway.info().current().getVersion().getVersion()
                : "none"));
    }

    private static String env(String key, String defaultValue) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? defaultValue : v;
    }
}
