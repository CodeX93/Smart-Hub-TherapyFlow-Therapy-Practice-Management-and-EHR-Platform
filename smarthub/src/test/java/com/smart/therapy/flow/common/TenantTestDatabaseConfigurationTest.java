package com.smart.therapy.flow.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantTestDatabaseConfigurationTest {
    @Test
    void tenantDatabaseExposesValidatedHarnessSettings() {
        TenantTestDatabase.ConnectionSettings settings = TenantTestDatabase.resolveSettings(Map.of(
                    "DB_URL", "jdbc:postgresql://localhost:5432/therapyflow_test",
                    "DB_USERNAME", "postgres",
                    "DB_PASSWORD", "generated-only",
                    "QA_MANAGED_DATABASE", "sh-t00"));
        assertThat(settings.url()).isEqualTo("jdbc:postgresql://localhost:5432/therapyflow_test");
    }

    @Test
    void rejectsNonLoopbackDatabaseSettings() {
        assertThatThrownBy(() -> TenantTestDatabase.resolveSettings(Map.of(
                "DB_URL", "jdbc:postgresql://database.example/therapyflow_test",
                "DB_USERNAME", "postgres",
                "DB_PASSWORD", "generated-only",
                "QA_MANAGED_DATABASE", "sh-t00")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Tenant fixture database must be a local disposable PostgreSQL instance");
    }
}
