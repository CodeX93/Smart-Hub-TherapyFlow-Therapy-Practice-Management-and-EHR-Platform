package com.smart.therapy.flow.migration.clienthub;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smart.therapy.flow.migration.clienthub.ClientHubMigrationReconciliationService.ReconciliationReport;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientHubMigrationRunRecorder {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    Long startRun(SourceInventory source, TargetInventory target, boolean executeMode) {
        if (!target.resolved()) {
            return null;
        }
        Map<String, Long> sourceCounts = source.tables().stream()
                .collect(Collectors.toMap(
                        table -> table.tableName(),
                        table -> table.rowCount(),
                        (left, right) -> left,
                        LinkedHashMap::new))
                .entrySet()
                .stream()
                .sorted(Map.Entry.comparingByKey())
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue,
                        (left, right) -> left,
                        LinkedHashMap::new));
        return jdbcTemplate.queryForObject("""
                INSERT INTO public.clienthub_migration_runs (
                    organisation_id, source_system, mode, status,
                    source_schema_fingerprint_sha256, source_counts, created_by
                )
                VALUES (?, 'ClientHubAI', ?, 'RUNNING', ?, ?::jsonb, 0)
                RETURNING id
                """,
                Long.class,
                target.organisationId(),
                executeMode ? "EXECUTE" : "DRY_RUN",
                source.schemaFingerprintSha256(),
                writeJson(sourceCounts));
    }

    void markSucceeded(Long runId) {
        markSucceeded(runId, null);
    }

    void markSucceeded(Long runId, ReconciliationReport report) {
        if (runId == null) {
            return;
        }
        if (report == null) {
            jdbcTemplate.update("""
                    UPDATE public.clienthub_migration_runs
                    SET status = 'SUCCEEDED',
                        completed_at = CURRENT_TIMESTAMP
                    WHERE id = ?
                    """,
                    runId);
            return;
        }
        jdbcTemplate.update("""
                UPDATE public.clienthub_migration_runs
                SET status = 'SUCCEEDED',
                    completed_at = CURRENT_TIMESTAMP,
                    target_counts = ?::jsonb,
                    totals = ?::jsonb
                WHERE id = ?
                """,
                writeJson(targetCounts(report)),
                writeJson(totals(report)),
                runId);
    }

    void markFailed(Long runId, Exception ex) {
        if (runId == null) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE public.clienthub_migration_runs
                SET status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP,
                    error_code = ?,
                    error_message = ?
                WHERE id = ?
                """,
                ex.getClass().getSimpleName(),
                truncate(ex.getMessage(), 1000),
                runId);
    }

    void recordOutcome(
            Long runId,
            TargetInventory target,
            String entityName,
            String sourceId,
            String action,
            String status,
            String errorCode,
            String errorMessage) {
        if (runId == null || !target.resolved()) {
            return;
        }
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_migration_record_outcomes (
                    run_id, organisation_id, entity_name, source_id,
                    action, status, error_code, error_message
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (run_id, entity_name, source_id)
                DO UPDATE SET
                    action = EXCLUDED.action,
                    status = EXCLUDED.status,
                    error_code = EXCLUDED.error_code,
                    error_message = EXCLUDED.error_message
                """,
                runId,
                target.organisationId(),
                entityName,
                sourceId,
                action,
                status,
                errorCode,
                truncate(errorMessage, 1000));
    }

    void linkMappingsToRun(Long runId, TargetInventory target) {
        if (runId == null || !target.resolved()) {
            return;
        }
        jdbcTemplate.update("""
                UPDATE public.clienthub_legacy_id_mappings
                SET first_seen_run_id = COALESCE(first_seen_run_id, ?),
                    last_seen_run_id = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                """,
                runId,
                runId,
                target.organisationId());
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private String writeJson(Map<String, Long> sourceCounts) {
        try {
            return objectMapper.writeValueAsString(sourceCounts);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Unable to serialize ClientHubAI source counts", ex);
        }
    }

    private Map<String, Long> targetCounts(ReconciliationReport report) {
        return report.entities().stream()
                .sorted((left, right) -> left.entityName().compareTo(right.entityName()))
                .collect(Collectors.toMap(
                        entity -> entity.entityName(),
                        entity -> entity.mappedRows(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    }

    private Map<String, Long> totals(ReconciliationReport report) {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("sourceRows", report.totalSourceRows());
        totals.put("mappedRows", report.totalMappedRows());
        totals.put("unmappedEstimate", report.entities().stream()
                .mapToLong(ClientHubMigrationReconciliationService.EntityReconciliation::unmappedEstimate)
                .sum());
        totals.put("pendingSyncEvents", report.totalPendingSyncEvents());
        totals.put("failedOrDeadSyncEvents", report.totalFailedSyncEvents());
        return totals;
    }
}
