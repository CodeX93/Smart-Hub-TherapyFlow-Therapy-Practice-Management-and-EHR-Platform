package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionOptionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentQuestionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentReportRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentResponseRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentSectionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceAssessmentTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceBillingRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceChecklistTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistItemRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientChecklistRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientPortalRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceClientRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceDocumentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePatientConsentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourcePaymentTransactionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceRoomRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionNoteRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceStaffUserRef;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSupervisorAssignmentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationPreferenceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTriggerRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceScheduledNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskCommentRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTaskRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTherapistBlockedTimeRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserProfileScheduleRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceUserZoomIntegrationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class ClientHubSyncEventProcessorService {

    private static final int DEAD_LETTER_AFTER_ATTEMPTS = 5;

    private final JdbcTemplate jdbcTemplate;
    private final ClientHubSourceInventoryService inventoryService;
    private final ClientHubStaffAuthExecuteService staffAuthExecuteService;
    private final ClientHubClientExecuteService clientExecuteService;
    private final ClientHubServiceExecuteService serviceExecuteService;
    private final ClientHubRoomIntegrationExecuteService roomIntegrationExecuteService;
    private final ClientHubTherapistScheduleExecuteService therapistScheduleExecuteService;
    private final ClientHubSessionExecuteService sessionExecuteService;
    private final ClientHubDocumentExecuteService documentExecuteService;
    private final ClientHubBillingExecuteService billingExecuteService;
    private final ClientHubTaskExecuteService taskExecuteService;
    private final ClientHubNotificationExecuteService notificationExecuteService;
    private final ClientHubClinicalExtrasExecuteService clinicalExtrasExecuteService;
    private final ClientHubAssessmentsExecuteService assessmentsExecuteService;
    private final ClientHubTranscriptsExecuteService transcriptsExecuteService;

    SyncProcessResult process(ClientHubMigrationProperties properties, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for ClientHubAI sync processing");
        }
        List<SyncEventRef> events = loadPendingEvents(target.organisationId(), properties.getSyncBatchSize());
        int succeeded = 0;
        int failed = 0;
        int deadLettered = 0;
        SourceLoadCache sourceCache = new SourceLoadCache();

        for (SyncEventRef event : events) {
            markRunning(event.id());
            try {
                applyEvent(properties, target, event, sourceCache);
                markSucceeded(event.id(), resolveTargetMapping(target, event));
                succeeded++;
            } catch (RuntimeException | java.sql.SQLException ex) {
                if (isMissingSource(ex) || event.attemptCount() + 1 >= DEAD_LETTER_AFTER_ATTEMPTS) {
                    markDeadLetter(event.id(), ex);
                    deadLettered++;
                } else {
                    markFailed(event.id(), ex);
                    failed++;
                }
            }
        }

        return new SyncProcessResult(events.size(), succeeded, failed, deadLettered);
    }

    private static boolean isMissingSource(Exception ex) {
        String message = ex.getMessage();
        return message != null && message.contains("Source row not found");
    }

    private List<SyncEventRef> loadPendingEvents(Long organisationId, int limit) {
        return jdbcTemplate.query("""
                SELECT id, entity_name, source_id, event_type, attempt_count
                FROM public.clienthub_sync_events
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND status = 'PENDING'
                  AND (next_attempt_at IS NULL OR next_attempt_at <= CURRENT_TIMESTAMP)
                ORDER BY created_at ASC, id ASC
                LIMIT ?
                """,
                (rs, rowNum) -> new SyncEventRef(
                        rs.getLong("id"),
                        rs.getString("entity_name"),
                        rs.getString("source_id"),
                        rs.getString("event_type"),
                        rs.getInt("attempt_count")),
                organisationId,
                Math.max(1, limit));
    }

    void applyEvent(ClientHubMigrationProperties properties, TargetInventory target, SyncEventRef event)
            throws java.sql.SQLException {
        applyEvent(properties, target, event, new SourceLoadCache());
    }

    private void applyEvent(
            ClientHubMigrationProperties properties,
            TargetInventory target,
            SyncEventRef event,
            SourceLoadCache sourceCache) throws java.sql.SQLException {
        if (!"UPDATE".equals(event.eventType()) && !"CREATE".equals(event.eventType())) {
            throw new IllegalStateException("ClientHubAI sync event type is not apply-enabled yet: " + event.eventType());
        }
        switch (event.entityName()) {
            case "users" -> staffAuthExecuteService.execute(
                    one(sourceCache, "users",
                            () -> inventoryService.loadStaffUserRefs(properties),
                            SourceStaffUserRef::legacyUserId, event.sourceId()),
                    target);
            case "clients" -> clientExecuteService.execute(
                    one(sourceCache, "clients",
                            () -> inventoryService.loadClientRecords(properties),
                            SourceClientRecord::legacyClientPk, event.sourceId()),
                    target);
            case "services" -> serviceExecuteService.execute(
                    one(sourceCache, "services",
                            () -> inventoryService.loadServiceRecords(properties),
                            SourceServiceRecord::legacyServicePk, event.sourceId()),
                    target);
            case "rooms" -> roomIntegrationExecuteService.execute(
                    one(sourceCache, "rooms",
                            () -> inventoryService.loadRoomRecords(properties),
                            SourceRoomRecord::legacyRoomPk, event.sourceId()),
                    List.of(),
                    target);
            case "user_profiles" -> therapistScheduleExecuteService.execute(
                    one(sourceCache, "user_profiles",
                            () -> inventoryService.loadUserProfileScheduleRecords(properties),
                            SourceUserProfileScheduleRecord::legacyProfilePk,
                            event.sourceId()),
                    List.of(),
                    target);
            case "therapist_blocked_times" -> therapistScheduleExecuteService.execute(
                    List.of(),
                    one(sourceCache, "therapist_blocked_times",
                            () -> inventoryService.loadTherapistBlockedTimeRecords(properties),
                            SourceTherapistBlockedTimeRecord::legacyBlockedPk,
                            event.sourceId()),
                    target);
            case "session_integrations" -> roomIntegrationExecuteService.execute(
                    List.of(),
                    one(sourceCache, "session_integrations",
                            () -> inventoryService.loadSessionIntegrationRecords(properties),
                            SourceSessionIntegrationRecord::legacyIntegrationKey,
                            event.sourceId()),
                    target);
            case "sessions" -> sessionExecuteService.execute(
                    one(sourceCache, "sessions",
                            () -> inventoryService.loadSessionRecords(properties),
                            SourceSessionRecord::legacySessionPk, event.sourceId()),
                    List.of(),
                    target);
            case "session_notes" -> sessionExecuteService.execute(
                    List.of(),
                    one(sourceCache, "session_notes",
                            () -> inventoryService.loadSessionNoteRecords(properties),
                            SourceSessionNoteRecord::legacyNotePk, event.sourceId()),
                    target);
            case "documents" -> documentExecuteService.execute(
                    one(sourceCache, "documents",
                            () -> inventoryService.loadDocumentRecords(properties),
                            SourceDocumentRecord::legacyDocumentPk, event.sourceId()),
                    target);
            case "session_billing" -> billingExecuteService.execute(
                    one(sourceCache, "session_billing",
                            () -> inventoryService.loadBillingRecords(
                                    properties,
                                    ClientHubSourceInventoryService.billingDateZone(target, properties)),
                            SourceBillingRecord::legacyBillingPk, event.sourceId()),
                    List.of(),
                    target);
            case "payment_transactions" -> billingExecuteService.execute(
                    List.of(),
                    one(sourceCache, "payment_transactions",
                            () -> inventoryService.loadPaymentTransactionRecords(properties),
                            SourcePaymentTransactionRecord::legacyPaymentTransactionPk,
                            event.sourceId()),
                    target);
            case "tasks" -> taskExecuteService.execute(
                    one(sourceCache, "tasks",
                            () -> inventoryService.loadTaskRecords(properties),
                            SourceTaskRecord::legacyTaskPk, event.sourceId()),
                    List.of(),
                    target);
            case "task_comments" -> taskExecuteService.execute(
                    List.of(),
                    one(sourceCache, "task_comments",
                            () -> inventoryService.loadTaskCommentRecords(properties),
                            SourceTaskCommentRecord::legacyCommentPk,
                            event.sourceId()),
                    target);
            case "notification_templates" -> notificationExecuteService.execute(
                    one(sourceCache, "notification_templates",
                            () -> inventoryService.loadNotificationTemplateRecords(properties),
                            SourceNotificationTemplateRecord::legacyTemplatePk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(),
                    target);
            case "notification_triggers" -> notificationExecuteService.execute(
                    List.of(),
                    one(sourceCache, "notification_triggers",
                            () -> inventoryService.loadNotificationTriggerRecords(properties),
                            SourceNotificationTriggerRecord::legacyTriggerPk, event.sourceId()),
                    List.of(), List.of(), List.of(),
                    target);
            case "notification_preferences" -> notificationExecuteService.execute(
                    List.of(), List.of(),
                    one(sourceCache, "notification_preferences",
                            () -> inventoryService.loadNotificationPreferenceRecords(properties),
                            SourceNotificationPreferenceRecord::legacyPreferencePk, event.sourceId()),
                    List.of(), List.of(),
                    target);
            case "notifications" -> notificationExecuteService.execute(
                    List.of(), List.of(), List.of(),
                    one(sourceCache, "notifications",
                            () -> inventoryService.loadNotificationRecords(properties),
                            SourceNotificationRecord::legacyNotificationPk, event.sourceId()),
                    List.of(),
                    target);
            case "scheduled_notifications" -> notificationExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "scheduled_notifications",
                            () -> inventoryService.loadScheduledNotificationRecords(properties),
                            SourceScheduledNotificationRecord::legacyScheduledPk, event.sourceId()),
                    target);
            case "patient_consents" -> clinicalExtrasExecuteService.execute(
                    one(sourceCache, "patient_consents",
                            () -> inventoryService.loadPatientConsentRecords(properties),
                            SourcePatientConsentRecord::legacyConsentPk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    target);
            case "supervisor_assignments" -> clinicalExtrasExecuteService.execute(
                    List.of(),
                    one(sourceCache, "supervisor_assignments",
                            () -> inventoryService.loadSupervisorAssignmentRecords(properties),
                            SourceSupervisorAssignmentRecord::legacyAssignmentPk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    target);
            case "client_portal" -> {
                List<SourceClientPortalRecord> allPortal = sourceCache.get(
                        "client_portal",
                        () -> inventoryService.loadClientPortalRecords(properties));
                List<SourceClientPortalRecord> portalRows = allPortal.stream()
                        .filter(row -> event.sourceId().equals(row.legacyClientPk()))
                        .toList();
                if (!portalRows.isEmpty()) {
                    clinicalExtrasExecuteService.execute(
                            List.of(), List.of(), portalRows,
                            List.of(), List.of(), List.of(), List.of(), List.of(),
                            target);
                }
            }
            case "checklist_templates" -> clinicalExtrasExecuteService.execute(
                    List.of(), List.of(), List.of(),
                    one(sourceCache, "checklist_templates",
                            () -> inventoryService.loadChecklistTemplateRecords(properties),
                            SourceChecklistTemplateRecord::legacyTemplatePk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(),
                    target);
            case "checklist_items" -> clinicalExtrasExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "checklist_items",
                            () -> inventoryService.loadChecklistItemRecords(properties),
                            SourceChecklistItemRecord::legacyItemPk, event.sourceId()),
                    List.of(), List.of(), List.of(),
                    target);
            case "client_checklists" -> clinicalExtrasExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "client_checklists",
                            () -> inventoryService.loadClientChecklistRecords(properties),
                            SourceClientChecklistRecord::legacyChecklistPk, event.sourceId()),
                    List.of(), List.of(),
                    target);
            case "client_checklist_items" -> clinicalExtrasExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "client_checklist_items",
                            () -> inventoryService.loadClientChecklistItemRecords(properties),
                            SourceClientChecklistItemRecord::legacyChecklistItemPk, event.sourceId()),
                    List.of(),
                    target);
            case "user_integrations_zoom" -> {
                List<SourceUserZoomIntegrationRecord> allZoom = sourceCache.get(
                        "user_integrations_zoom",
                        () -> inventoryService.loadUserZoomIntegrationRecords(properties));
                List<SourceUserZoomIntegrationRecord> zoomRows = allZoom.stream()
                        .filter(row -> event.sourceId().equals(row.userLegacyId()))
                        .toList();
                if (!zoomRows.isEmpty()) {
                    clinicalExtrasExecuteService.execute(
                            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                            zoomRows,
                            target);
                }
            }
            case "assessment_templates" -> assessmentsExecuteService.execute(
                    one(sourceCache, "assessment_templates",
                            () -> inventoryService.loadAssessmentTemplateRecords(properties),
                            SourceAssessmentTemplateRecord::legacyTemplatePk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    target);
            case "assessment_sections" -> assessmentsExecuteService.execute(
                    List.of(),
                    one(sourceCache, "assessment_sections",
                            () -> inventoryService.loadAssessmentSectionRecords(properties),
                            SourceAssessmentSectionRecord::legacySectionPk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    target);
            case "assessment_questions" -> assessmentsExecuteService.execute(
                    List.of(), List.of(),
                    one(sourceCache, "assessment_questions",
                            () -> inventoryService.loadAssessmentQuestionRecords(properties),
                            SourceAssessmentQuestionRecord::legacyQuestionPk, event.sourceId()),
                    List.of(), List.of(), List.of(), List.of(),
                    target);
            case "assessment_question_options" -> assessmentsExecuteService.execute(
                    List.of(), List.of(), List.of(),
                    one(sourceCache, "assessment_question_options",
                            () -> inventoryService.loadAssessmentQuestionOptionRecords(properties),
                            SourceAssessmentQuestionOptionRecord::legacyOptionPk, event.sourceId()),
                    List.of(), List.of(), List.of(),
                    target);
            case "assessment_assignments" -> assessmentsExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "assessment_assignments",
                            () -> inventoryService.loadAssessmentAssignmentRecords(properties),
                            SourceAssessmentAssignmentRecord::legacyAssignmentPk, event.sourceId()),
                    List.of(), List.of(),
                    target);
            case "assessment_responses" -> assessmentsExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "assessment_responses",
                            () -> inventoryService.loadAssessmentResponseRecords(properties),
                            SourceAssessmentResponseRecord::legacyResponsePk, event.sourceId()),
                    List.of(),
                    target);
            case "assessment_reports" -> assessmentsExecuteService.execute(
                    List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
                    one(sourceCache, "assessment_reports",
                            () -> inventoryService.loadAssessmentReportRecords(properties),
                            SourceAssessmentReportRecord::legacyReportPk, event.sourceId()),
                    target);
            case "session_transcripts" -> transcriptsExecuteService.execute(
                    one(sourceCache, "session_transcripts",
                            () -> inventoryService.loadTranscriptRecords(properties),
                            SourceTranscriptRecord::legacyTranscriptPk, event.sourceId()),
                    target);
            default -> throw new IllegalArgumentException("Unsupported ClientHubAI sync entity: " + event.entityName());
        }
    }

    private <T> List<T> one(
            SourceLoadCache cache,
            String cacheKey,
            SourceLoader<T> loader,
            Function<T, String> idExtractor,
            String sourceId) throws java.sql.SQLException {
        return one(cache.get(cacheKey, loader), idExtractor, sourceId);
    }

    private <T> List<T> one(List<T> rows, Function<T, String> idExtractor, String sourceId) {
        return rows.stream()
                .filter(row -> sourceId.equals(idExtractor.apply(row)))
                .findFirst()
                .map(List::of)
                .orElseThrow(() -> new IllegalStateException("Source row not found for sync event source id " + sourceId));
    }

    @FunctionalInterface
    private interface SourceLoader<T> {
        List<T> load() throws java.sql.SQLException;
    }

    private static final class SourceLoadCache {
        private final Map<String, Object> values = new HashMap<>();

        @SuppressWarnings("unchecked")
        <T> List<T> get(String key, SourceLoader<T> loader) throws java.sql.SQLException {
            Object existing = values.get(key);
            if (existing != null) {
                return (List<T>) existing;
            }
            List<T> loaded = loader.load();
            values.put(key, loaded);
            return loaded;
        }
    }

    private void markRunning(Long eventId) {
        jdbcTemplate.update("""
                UPDATE public.clienthub_sync_events
                SET status = 'RUNNING',
                    attempt_count = attempt_count + 1,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                eventId);
    }

    Optional<TargetMappingRef> resolveTargetMapping(TargetInventory target, SyncEventRef event) {
        return jdbcTemplate.query("""
                SELECT target_table, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                  AND source_id = ?
                """,
                (rs, rowNum) -> new TargetMappingRef(rs.getString("target_table"), rs.getLong("target_id")),
                target.organisationId(),
                event.entityName(),
                event.sourceId())
                .stream()
                .findFirst();
    }

    private void markSucceeded(Long eventId, Optional<TargetMappingRef> targetMapping) {
        jdbcTemplate.update("""
                UPDATE public.clienthub_sync_events
                SET status = 'SUCCEEDED',
                    target_table = ?,
                    target_id = ?,
                    error_code = NULL,
                    error_message = NULL,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                targetMapping.map(TargetMappingRef::targetTable).orElse(null),
                targetMapping.map(TargetMappingRef::targetId).orElse(null),
                eventId);
    }

    private void markFailed(Long eventId, Exception ex) {
        jdbcTemplate.update("""
                UPDATE public.clienthub_sync_events
                SET status = 'PENDING',
                    next_attempt_at = CURRENT_TIMESTAMP + INTERVAL '5 minutes',
                    error_code = ?,
                    error_message = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                ex.getClass().getSimpleName(),
                truncate(ex.getMessage(), 500),
                eventId);
    }

    private void markDeadLetter(Long eventId, Exception ex) {
        jdbcTemplate.update("""
                UPDATE public.clienthub_sync_events
                SET status = 'DEAD_LETTER',
                    error_code = ?,
                    error_message = ?,
                    updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """,
                ex.getClass().getSimpleName(),
                truncate(ex.getMessage(), 500),
                eventId);
    }

    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    record SyncEventRef(Long id, String entityName, String sourceId, String eventType, int attemptCount) {
    }

    record TargetMappingRef(String targetTable, Long targetId) {
    }

    record SyncProcessResult(int claimed, int succeeded, int failed, int deadLettered) {
    }
}
