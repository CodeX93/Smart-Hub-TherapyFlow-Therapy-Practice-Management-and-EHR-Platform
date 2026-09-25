package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Service
@RequiredArgsConstructor
public class ClientHubSyncScannerService {

    private static final List<SyncEntityDefinition> ENTITIES = List.of(
            new SyncEntityDefinition("users", "users", "updated_at", "users", null),
            new SyncEntityDefinition("clients", "clients", "updated_at", "clients", null),
            new SyncEntityDefinition("services", "services", "updated_at", "services", "is_active = false"),
            new SyncEntityDefinition("rooms", "rooms", "updated_at", "rooms", "is_active = false"),
            new SyncEntityDefinition("user_profiles", "user_profiles", "updated_at", "user_profiles", null),
            new SyncEntityDefinition("therapist_blocked_times", "therapist_blocked_times", "updated_at",
                    "therapist_blocked_times", "is_active = false"),
            new SyncEntityDefinition("sessions", "sessions", "updated_at", "sessions", null),
            new SyncEntityDefinition("session_notes", "session_notes", "updated_at", "session_notes", null),
            new SyncEntityDefinition("documents", "documents", "created_at", "documents", null),
            new SyncEntityDefinition("session_billing", "session_billing", "updated_at", "session_billing", null),
            new SyncEntityDefinition("payment_transactions", "payment_transactions", "recorded_at", "payment_transactions", null),
            new SyncEntityDefinition("tasks", "tasks", "updated_at", "tasks", null),
            new SyncEntityDefinition("task_comments", "task_comments", "updated_at", "task_comments", null),
            new SyncEntityDefinition("notification_templates", "notification_templates", "updated_at",
                    "notification_templates", null),
            new SyncEntityDefinition("notification_triggers", "notification_triggers", "updated_at",
                    "notification_triggers", null),
            new SyncEntityDefinition("notification_preferences", "notification_preferences", "updated_at",
                    "notification_preferences", null),
            new SyncEntityDefinition("notifications", "notifications", "created_at", "notifications", null),
            new SyncEntityDefinition("scheduled_notifications", "scheduled_notifications", "created_at",
                    "scheduled_notifications", null),
            new SyncEntityDefinition("patient_consents", "patient_consents", "updated_at", "patient_consents", null),
            new SyncEntityDefinition("supervisor_assignments", "supervisor_assignments", "updated_at",
                    "supervisor_assignments", "is_active = false"),
            new SyncEntityDefinition("client_portal", "clients", "updated_at", "client_portal", null),
            new SyncEntityDefinition("checklist_templates", "checklist_templates", "updated_at",
                    "checklist_templates", "is_active = false"),
            new SyncEntityDefinition("checklist_items", "checklist_items", "created_at", "checklist_items", null),
            new SyncEntityDefinition("client_checklists", "client_checklists", "created_at",
                    "client_checklists", null),
            new SyncEntityDefinition("client_checklist_items", "client_checklist_items", "created_at",
                    "client_checklist_items", null),
            new SyncEntityDefinition("user_integrations_zoom", "users", "updated_at",
                    "user_integrations_zoom", null),
            new SyncEntityDefinition("assessment_templates", "assessment_templates", "updated_at",
                    "assessment_templates", "is_active = false"),
            new SyncEntityDefinition("assessment_sections", "assessment_sections", "updated_at",
                    "assessment_sections", null),
            new SyncEntityDefinition("assessment_questions", "assessment_questions", "updated_at",
                    "assessment_questions", null),
            // V1 assessment_question_options has no timestamp cursor column; options are
            // re-applied with question sync / bulk assessments execute.
            new SyncEntityDefinition("assessment_assignments", "assessment_assignments", "updated_at",
                    "assessment_assignments", null),
            new SyncEntityDefinition("assessment_responses", "assessment_responses", "updated_at",
                    "assessment_responses", null),
            new SyncEntityDefinition("assessment_reports", "assessment_reports", "generated_at",
                    "assessment_reports", null),
            new SyncEntityDefinition("session_transcripts", "session_transcripts", "updated_at",
                    "session_transcripts", null)
    );

    private final JdbcTemplate jdbcTemplate;

    SyncScanResult scan(ClientHubMigrationProperties properties, TargetInventory target, boolean enqueueEvents)
            throws SQLException {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for ClientHubAI sync scanning");
        }

        List<EntitySyncScan> scans = new ArrayList<>();
        try (Connection connection = openReadOnlyConnection(properties)) {
            connection.setAutoCommit(false);
            try {
                for (SyncEntityDefinition entity : ENTITIES) {
                    Instant cursor = loadCursor(target.organisationId(), entity.entityName());
                    List<SourceChange> changes = loadChanges(connection, properties, entity, cursor, properties.getSyncBatchSize());
                    if (enqueueEvents && !changes.isEmpty()) {
                        enqueueChanges(target, entity, changes);
                        updateCheckpoint(target.organisationId(), entity.entityName(), changes.get(changes.size() - 1));
                    }
                    scans.add(new EntitySyncScan(
                            entity.entityName(),
                            entity.sourceTable(),
                            entity.cursorColumn(),
                            cursor,
                            changes.size(),
                            changes.isEmpty() ? null : changes.get(changes.size() - 1).cursorValue()));
                }
                connection.rollback();
                return new SyncScanResult(scans, enqueueEvents);
            } catch (SQLException | RuntimeException ex) {
                connection.rollback();
                throw ex;
            }
        }
    }

    private List<SourceChange> loadChanges(
            Connection connection,
            ClientHubMigrationProperties properties,
            SyncEntityDefinition entity,
            Instant cursor,
            int limit) throws SQLException {
        String table = ClientHubIdentifier.qualified(properties.getSourceSchema(), entity.sourceTable());
        String cursorColumn = ClientHubIdentifier.quote(entity.cursorColumn());
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, %s AS cursor_value, %s AS event_type
                FROM %s
                WHERE %s > ?
                ORDER BY %s ASC, id ASC
                LIMIT ?
                """.formatted(cursorColumn, eventTypeExpression(entity), table, cursorColumn, cursorColumn))) {
            statement.setTimestamp(1, Timestamp.from(cursor));
            statement.setInt(2, Math.max(1, limit));
            try (ResultSet rows = statement.executeQuery()) {
                List<SourceChange> changes = new ArrayList<>();
                while (rows.next()) {
                    Instant cursorValue = rows.getTimestamp("cursor_value").toInstant();
                    changes.add(new SourceChange(
                            String.valueOf(rows.getLong("id")),
                            cursorValue,
                            rows.getString("event_type")));
                }
                return changes;
            }
        }
    }

    String eventTypeExpression(SyncEntityDefinition entity) {
        if (entity.archiveCondition() == null || entity.archiveCondition().isBlank()) {
            return "'UPDATE'";
        }
        return "CASE WHEN " + entity.archiveCondition() + " THEN 'ARCHIVE' ELSE 'UPDATE' END";
    }

    private Instant loadCursor(Long organisationId, String entityName) {
        List<String> cursors = jdbcTemplate.query("""
                SELECT cursor_value
                FROM public.clienthub_sync_checkpoints
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getString("cursor_value"),
                organisationId,
                entityName);
        if (cursors.isEmpty() || cursors.get(0) == null || cursors.get(0).isBlank()) {
            return Instant.EPOCH;
        }
        return Instant.parse(cursors.get(0));
    }

    private void enqueueChanges(TargetInventory target, SyncEntityDefinition entity, List<SourceChange> changes) {
        for (SourceChange change : changes) {
            String idempotencyKey = entity.entityName() + ":" + change.sourceId() + ":" + change.cursorValue()
                    + ":" + change.eventType();
            jdbcTemplate.update("""
                    INSERT INTO public.clienthub_sync_events (
                        organisation_id, source_system, entity_name, source_id, idempotency_key,
                        event_type, status, next_attempt_at, source_checksum_sha256, target_table
                    )
                    VALUES (?, 'ClientHubAI', ?, ?, ?, ?, 'PENDING', CURRENT_TIMESTAMP, ?, ?)
                    ON CONFLICT (organisation_id, idempotency_key) DO NOTHING
                    """,
                    target.organisationId(),
                    entity.entityName(),
                    change.sourceId(),
                    idempotencyKey,
                    change.eventType(),
                    ClientHubIdentifier.sha256Hex(idempotencyKey),
                    entity.targetTable());
        }
    }

    private void updateCheckpoint(Long organisationId, String entityName, SourceChange lastChange) {
        String checksum = ClientHubIdentifier.sha256Hex(entityName + "|" + lastChange.sourceId() + "|" + lastChange.cursorValue());
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_sync_checkpoints (
                    organisation_id, source_system, entity_name, cursor_value, cursor_checksum_sha256,
                    last_successful_sync_at, last_attempted_sync_at, status, failure_count
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'SUCCEEDED', 0)
                ON CONFLICT (organisation_id, source_system, entity_name)
                DO UPDATE SET
                    cursor_value = EXCLUDED.cursor_value,
                    cursor_checksum_sha256 = EXCLUDED.cursor_checksum_sha256,
                    last_successful_sync_at = CURRENT_TIMESTAMP,
                    last_attempted_sync_at = CURRENT_TIMESTAMP,
                    status = 'SUCCEEDED',
                    failure_count = 0,
                    last_error_code = NULL,
                    last_error_message = NULL,
                    updated_at = CURRENT_TIMESTAMP
                """,
                organisationId,
                entityName,
                lastChange.cursorValue().toString(),
                checksum);
    }

    private Connection openReadOnlyConnection(ClientHubMigrationProperties properties) throws SQLException {
        DriverManager.setLoginTimeout(properties.getConnectTimeoutSeconds());
        Properties connectionProperties = new Properties();
        connectionProperties.put("user", properties.getSourceUsername());
        if (properties.getSourcePassword() != null && !properties.getSourcePassword().isBlank()) {
            connectionProperties.put("password", properties.getSourcePassword());
        }
        Connection connection = DriverManager.getConnection(properties.getSourceUrl(), connectionProperties);
        connection.setReadOnly(true);
        return connection;
    }

    record SyncEntityDefinition(
            String entityName,
            String sourceTable,
            String cursorColumn,
            String targetTable,
            String archiveCondition) {
    }

    record SourceChange(String sourceId, Instant cursorValue, String eventType) {
    }

    record EntitySyncScan(
            String entityName,
            String sourceTable,
            String cursorColumn,
            Instant previousCursor,
            int changedRows,
            Instant nextCursor) {
    }

    record SyncScanResult(List<EntitySyncScan> entities, boolean enqueued) {

        int totalChangedRows() {
            return entities.stream().mapToInt(EntitySyncScan::changedRows).sum();
        }
    }
}
