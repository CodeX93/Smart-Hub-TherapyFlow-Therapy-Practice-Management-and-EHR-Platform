package com.smart.therapy.flow.migration.clienthub;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ClientHubControlTableReadinessServiceTest {

    @Test
    void passesWhenAllControlTablesExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn(ClientHubControlTableReadinessService.REQUIRED_TABLES);

        new ClientHubControlTableReadinessService(jdbcTemplate).assertReady();
    }

    @Test
    void failsWithClearMessageWhenControlTablesAreMissing() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForList(any(String.class), eq(String.class), any(Object[].class)))
                .thenReturn(List.of(
                        "clienthub_migration_runs",
                        "clienthub_legacy_id_mappings"
                ));

        assertThatThrownBy(() -> new ClientHubControlTableReadinessService(jdbcTemplate).assertReady())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("clienthub_migration_record_outcomes")
                .hasMessageContaining("V91__clienthub_migration_control.sql");
    }
}
