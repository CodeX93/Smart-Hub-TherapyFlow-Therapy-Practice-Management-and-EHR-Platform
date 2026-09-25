package com.smart.therapy.flow.migration.clienthub;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
public class ClientHubControlTableReadinessService {

    static final List<String> REQUIRED_TABLES = List.of(
            "clienthub_migration_runs",
            "clienthub_legacy_id_mappings",
            "clienthub_migration_record_outcomes",
            "clienthub_sync_checkpoints",
            "clienthub_sync_events"
    );

    private final JdbcTemplate jdbcTemplate;

    void assertReady() {
        Set<String> existing = new TreeSet<>(jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                  AND table_name IN (?, ?, ?, ?, ?)
                """,
                String.class,
                REQUIRED_TABLES.toArray()));
        List<String> missing = REQUIRED_TABLES.stream()
                .filter(table -> !existing.contains(table))
                .toList();
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "ClientHubAI migration control tables are missing: " + String.join(", ", missing)
                            + ". Apply Flyway migration V91__clienthub_migration_control.sql before running migration or sync.");
        }
    }
}
