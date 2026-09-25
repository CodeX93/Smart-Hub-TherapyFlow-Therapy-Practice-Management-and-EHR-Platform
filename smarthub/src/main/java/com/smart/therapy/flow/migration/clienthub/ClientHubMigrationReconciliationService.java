package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceInventory;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ClientHubMigrationReconciliationService {

    private static final List<EntitySourceTable> ENTITIES = List.of(
            new EntitySourceTable("users", "users"),
            new EntitySourceTable("clients", "clients"),
            new EntitySourceTable("services", "services"),
            new EntitySourceTable("rooms", "rooms"),
            new EntitySourceTable("user_profiles", "user_profiles"),
            new EntitySourceTable("therapist_blocked_times", "therapist_blocked_times"),
            new EntitySourceTable("sessions", "sessions"),
            new EntitySourceTable("session_notes", "session_notes"),
            new EntitySourceTable("documents", "documents"),
            new EntitySourceTable("session_billing", "session_billing"),
            new EntitySourceTable("payment_transactions", "payment_transactions"),
            new EntitySourceTable("session_integrations", "sessions"),
            new EntitySourceTable("tasks", "tasks"),
            new EntitySourceTable("task_comments", "task_comments"),
            new EntitySourceTable("notification_templates", "notification_templates"),
            new EntitySourceTable("notification_triggers", "notification_triggers"),
            new EntitySourceTable("notification_preferences", "notification_preferences"),
            new EntitySourceTable("notifications", "notifications"),
            new EntitySourceTable("scheduled_notifications", "scheduled_notifications"),
            new EntitySourceTable("patient_consents", "patient_consents"),
            new EntitySourceTable("supervisor_assignments", "supervisor_assignments"),
            new EntitySourceTable("client_portal", "clients"),
            new EntitySourceTable("checklist_templates", "checklist_templates"),
            new EntitySourceTable("checklist_items", "checklist_items"),
            new EntitySourceTable("client_checklists", "client_checklists"),
            new EntitySourceTable("client_checklist_items", "client_checklist_items"),
            new EntitySourceTable("user_integrations_zoom", "users"),
            new EntitySourceTable("assessment_templates", "assessment_templates"),
            new EntitySourceTable("assessment_sections", "assessment_sections"),
            new EntitySourceTable("assessment_questions", "assessment_questions"),
            new EntitySourceTable("assessment_question_options", "assessment_question_options"),
            new EntitySourceTable("assessment_assignments", "assessment_assignments"),
            new EntitySourceTable("assessment_responses", "assessment_responses"),
            new EntitySourceTable("assessment_reports", "assessment_reports"),
            new EntitySourceTable("session_transcripts", "session_transcripts")
    );

    private final JdbcTemplate jdbcTemplate;

    ReconciliationReport buildReport(SourceInventory sourceInventory, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for ClientHubAI reconciliation");
        }
        Map<String, Long> sourceCounts = sourceInventory.tables().stream()
                .collect(Collectors.toMap(table -> table.tableName(), table -> table.rowCount(), (left, right) -> left));
        Map<String, Long> mappingCounts = loadMappingCounts(target.organisationId());
        Map<String, SyncStatusCounts> syncCounts = loadSyncCounts(target.organisationId());

        List<EntityReconciliation> entities = ENTITIES.stream()
                .map(entity -> {
                    long sourceRows = sourceCounts.getOrDefault(entity.sourceTable(), 0L);
                    long mappedRows = mappingCounts.getOrDefault(entity.entityName(), 0L);
                    SyncStatusCounts sync = syncCounts.getOrDefault(entity.entityName(), SyncStatusCounts.empty());
                    return new EntityReconciliation(
                            entity.entityName(),
                            entity.sourceTable(),
                            sourceRows,
                            mappedRows,
                            Math.max(sourceRows - mappedRows, 0),
                            sync.pending(),
                            sync.running(),
                            sync.failed(),
                            sync.deadLetter());
                })
                .toList();
        return new ReconciliationReport(entities);
    }

    private Map<String, Long> loadMappingCounts(Long organisationId) {
        return jdbcTemplate.query("""
                SELECT entity_name, COUNT(*) AS mappings
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                GROUP BY entity_name
                """,
                (rs, rowNum) -> Map.entry(rs.getString("entity_name"), rs.getLong("mappings")),
                organisationId)
                .stream()
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Map<String, SyncStatusCounts> loadSyncCounts(Long organisationId) {
        Map<String, List<SyncStatusCountRow>> rows = jdbcTemplate.query("""
                SELECT entity_name, status, COUNT(*) AS events
                FROM public.clienthub_sync_events
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                GROUP BY entity_name, status
                """,
                (rs, rowNum) -> new SyncStatusCountRow(
                        rs.getString("entity_name"),
                        rs.getString("status"),
                        rs.getLong("events")),
                organisationId)
                .stream()
                .collect(Collectors.groupingBy(SyncStatusCountRow::entityName));

        return rows.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> SyncStatusCounts.from(entry.getValue())));
    }

    record ReconciliationReport(List<EntityReconciliation> entities) {
        long totalSourceRows() {
            return entities.stream().mapToLong(EntityReconciliation::sourceRows).sum();
        }

        long totalMappedRows() {
            return entities.stream().mapToLong(EntityReconciliation::mappedRows).sum();
        }

        long totalPendingSyncEvents() {
            return entities.stream().mapToLong(EntityReconciliation::pendingSyncEvents).sum();
        }

        long totalFailedSyncEvents() {
            return entities.stream().mapToLong(entity -> entity.failedSyncEvents() + entity.deadLetterSyncEvents()).sum();
        }
    }

    record EntityReconciliation(
            String entityName,
            String sourceTable,
            long sourceRows,
            long mappedRows,
            long unmappedEstimate,
            long pendingSyncEvents,
            long runningSyncEvents,
            long failedSyncEvents,
            long deadLetterSyncEvents) {
    }

    private record EntitySourceTable(String entityName, String sourceTable) {
    }

    private record SyncStatusCountRow(String entityName, String status, long count) {
    }

    private record SyncStatusCounts(long pending, long running, long failed, long deadLetter) {
        static SyncStatusCounts empty() {
            return new SyncStatusCounts(0, 0, 0, 0);
        }

        static SyncStatusCounts from(List<SyncStatusCountRow> rows) {
            long pending = 0;
            long running = 0;
            long failed = 0;
            long deadLetter = 0;
            for (SyncStatusCountRow row : rows) {
                switch (row.status()) {
                    case "PENDING" -> pending += row.count();
                    case "RUNNING" -> running += row.count();
                    case "FAILED" -> failed += row.count();
                    case "DEAD_LETTER" -> deadLetter += row.count();
                    default -> {
                    }
                }
            }
            return new SyncStatusCounts(pending, running, failed, deadLetter);
        }
    }
}
