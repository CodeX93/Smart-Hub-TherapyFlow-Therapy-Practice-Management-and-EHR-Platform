package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationPreferenceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTemplateRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceNotificationTriggerRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceScheduledNotificationRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.notification.entity.NotificationPreference;
import com.smart.therapy.flow.notification.entity.NotificationTemplate;
import com.smart.therapy.flow.notification.entity.NotificationTrigger;
import com.smart.therapy.flow.notification.entity.ScheduledNotification;
import com.smart.therapy.flow.notification.enums.EntityType;
import com.smart.therapy.flow.notification.enums.NotificationType;
import com.smart.therapy.flow.notification.repository.NotificationPreferenceRepository;
import com.smart.therapy.flow.notification.repository.NotificationTemplateRepository;
import com.smart.therapy.flow.notification.repository.NotificationTriggerRepository;
import com.smart.therapy.flow.notification.repository.ScheduledNotificationRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubNotificationExecuteService {

    private static final int BATCH_SIZE = 200;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationTriggerRepository triggerRepository;
    private final NotificationPreferenceRepository preferenceRepository;
    private final ScheduledNotificationRepository scheduledNotificationRepository;
    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;

    public NotificationExecuteResult execute(
            List<SourceNotificationTemplateRecord> templates,
            List<SourceNotificationTriggerRecord> triggers,
            List<SourceNotificationPreferenceRecord> preferences,
            List<SourceNotificationRecord> notifications,
            List<SourceScheduledNotificationRecord> scheduled,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for notification execution");
        }

        ConfigBootstrap bootstrap = tenantTransactionExecutor.executeWrite(
                target.organisationId(),
                target.schemaName(),
                () -> executeConfigInTenant(templates, triggers, preferences, target));

        Set<String> alreadyMappedNotificationIds = loadMappedSourceIdSet(target.organisationId(), "notifications");
        List<SourceNotificationRecord> pendingNotifications = notifications.stream()
                .filter(source -> !alreadyMappedNotificationIds.contains(source.legacyNotificationPk()))
                .toList();
        log.info("ClientHubAI notification import queue: total={} already_mapped={} pending={}",
                notifications.size(), alreadyMappedNotificationIds.size(), pendingNotifications.size());

        Counts notificationCounts = new Counts();
        int alreadyInScope = notifications.size() - pendingNotifications.size();
        notificationCounts.updated = alreadyInScope;
        notificationCounts.mappedReruns = alreadyInScope;
        for (int offset = 0; offset < pendingNotifications.size(); offset += BATCH_SIZE) {
            List<SourceNotificationRecord> batch = pendingNotifications.subList(
                    offset, Math.min(offset + BATCH_SIZE, pendingNotifications.size()));
            Counts batchCounts = executeNotificationBatchWithRetry(
                    target,
                    batch,
                    bootstrap.userMappings(),
                    bootstrap.clientMappings(),
                    bootstrap.sessionMappings(),
                    bootstrap.taskMappings(),
                    bootstrap.documentMappings());
            notificationCounts.add(batchCounts);
            int processed = alreadyInScope + Math.min(offset + BATCH_SIZE, pendingNotifications.size());
            log.info("ClientHubAI notification import progress: processed={}/{} batch_created={}",
                    processed, notifications.size(), batchCounts.created);
        }

        Counts scheduledCounts = new Counts();
        for (int offset = 0; offset < scheduled.size(); offset += BATCH_SIZE) {
            List<SourceScheduledNotificationRecord> batch = scheduled.subList(
                    offset, Math.min(offset + BATCH_SIZE, scheduled.size()));
            Counts batchCounts = tenantTransactionExecutor.executeWrite(
                    target.organisationId(),
                    target.schemaName(),
                    () -> executeScheduledBatch(batch, target, bootstrap.triggerMappings(), bootstrap.sessionMappings(),
                            bootstrap.clientMappings(), bootstrap.taskMappings(), bootstrap.documentMappings(),
                            bootstrap.userMappings()));
            scheduledCounts.add(batchCounts);
        }

        return new NotificationExecuteResult(
                templates.size(), bootstrap.templatesCreated(), bootstrap.templatesUpdated(), bootstrap.templateMappedReruns(),
                triggers.size(), bootstrap.triggersCreated(), bootstrap.triggersUpdated(), bootstrap.triggerMappedReruns(),
                preferences.size(), bootstrap.preferencesCreated(), bootstrap.preferencesUpdated(),
                bootstrap.preferenceMappedReruns(), bootstrap.preferencesSkipped(),
                notifications.size(), notificationCounts.created, notificationCounts.updated, notificationCounts.mappedReruns,
                scheduled.size(), scheduledCounts.created, scheduledCounts.updated, scheduledCounts.mappedReruns,
                scheduledCounts.skipped);
    }

    private ConfigBootstrap executeConfigInTenant(
            List<SourceNotificationTemplateRecord> templates,
            List<SourceNotificationTriggerRecord> triggers,
            List<SourceNotificationPreferenceRecord> preferences,
            TargetInventory target) {
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");
        Map<String, Long> taskMappings = loadMappings(target.organisationId(), "tasks");
        Map<String, Long> documentMappings = loadMappings(target.organisationId(), "documents");
        Map<String, Long> templateMappings = loadMappings(target.organisationId(), "notification_templates");
        Map<String, Long> triggerMappings = loadMappings(target.organisationId(), "notification_triggers");
        Map<String, Long> preferenceMappings = loadMappings(target.organisationId(), "notification_preferences");
        Map<String, Long> templatesByName = loadTemplateIdsByName();
        Map<String, Long> triggersByName = loadTriggerIdsByName();

        int templatesCreated = 0;
        int templatesUpdated = 0;
        int templateMappedReruns = 0;
        for (SourceNotificationTemplateRecord source : templates) {
            Optional<Long> mappedId = Optional.ofNullable(templateMappings.get(source.legacyTemplatePk()));
            NotificationTemplate template = mappedId.flatMap(templateRepository::findById)
                    .orElseGet(() -> templatesByName.containsKey(source.name())
                            ? templateRepository.findById(templatesByName.get(source.name())).orElseGet(NotificationTemplate::new)
                            : new NotificationTemplate());
            boolean existing = template.getId() != null;
            applyTemplateFields(template, source);
            NotificationTemplate saved = templateRepository.save(template);
            templateMappings.put(source.legacyTemplatePk(), saved.getId());
            templatesByName.put(source.name(), saved.getId());
            upsertLegacyMapping(target, "notification_templates", "notification_templates",
                    source.legacyTemplatePk(), saved.getId(), templateChecksum(source));
            if (existing) {
                templatesUpdated++;
                if (mappedId.isPresent()) {
                    templateMappedReruns++;
                }
            } else {
                templatesCreated++;
            }
        }

        int triggersCreated = 0;
        int triggersUpdated = 0;
        int triggerMappedReruns = 0;
        for (SourceNotificationTriggerRecord source : triggers) {
            Optional<Long> mappedId = Optional.ofNullable(triggerMappings.get(source.legacyTriggerPk()));
            NotificationTrigger trigger = mappedId.flatMap(triggerRepository::findById)
                    .orElseGet(() -> triggersByName.containsKey(source.name())
                            ? triggerRepository.findById(triggersByName.get(source.name())).orElseGet(NotificationTrigger::new)
                            : new NotificationTrigger());
            boolean existing = trigger.getId() != null;
            applyTriggerFields(trigger, source, templateMappings);
            NotificationTrigger saved = triggerRepository.save(trigger);
            triggerMappings.put(source.legacyTriggerPk(), saved.getId());
            triggersByName.put(source.name(), saved.getId());
            upsertLegacyMapping(target, "notification_triggers", "notification_triggers",
                    source.legacyTriggerPk(), saved.getId(), triggerChecksum(source));
            if (existing) {
                triggersUpdated++;
                if (mappedId.isPresent()) {
                    triggerMappedReruns++;
                }
            } else {
                triggersCreated++;
            }
        }

        int preferencesCreated = 0;
        int preferencesUpdated = 0;
        int preferenceMappedReruns = 0;
        int preferencesSkipped = 0;
        for (SourceNotificationPreferenceRecord source : preferences) {
            if (ClientHubNotificationTypeMapper.isGlobalPreferenceTrigger(source.triggerType())
                    || ClientHubNotificationTypeMapper.mapType(source.triggerType()).isEmpty()) {
                preferencesSkipped++;
                continue;
            }
            Optional<Long> mappedId = Optional.ofNullable(preferenceMappings.get(source.legacyPreferencePk()));
            NotificationPreference preference = mappedId.flatMap(preferenceRepository::findById)
                    .orElseGet(NotificationPreference::new);
            boolean existing = preference.getId() != null;
            applyPreferenceFields(preference, source, userMappings);
            NotificationPreference saved = preferenceRepository.save(preference);
            preferenceMappings.put(source.legacyPreferencePk(), saved.getId());
            upsertLegacyMapping(target, "notification_preferences", "notification_preferences",
                    source.legacyPreferencePk(), saved.getId(), preferenceChecksum(source));
            if (existing) {
                preferencesUpdated++;
                if (mappedId.isPresent()) {
                    preferenceMappedReruns++;
                }
            } else {
                preferencesCreated++;
            }
        }

        return new ConfigBootstrap(
                userMappings, clientMappings, sessionMappings, taskMappings, documentMappings, triggerMappings,
                templatesCreated, templatesUpdated, templateMappedReruns,
                triggersCreated, triggersUpdated, triggerMappedReruns,
                preferencesCreated, preferencesUpdated, preferenceMappedReruns, preferencesSkipped);
    }

    private Counts executeNotificationBatchWithRetry(
            TargetInventory target,
            List<SourceNotificationRecord> batch,
            Map<String, Long> userMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return tenantTransactionExecutor.executeWrite(
                        target.organisationId(),
                        target.schemaName(),
                        () -> executeNotificationBatch(
                                batch,
                                target,
                                userMappings,
                                clientMappings,
                                sessionMappings,
                                taskMappings,
                                documentMappings));
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= 3 || !isTransientDbFailure(ex)) {
                    throw ex;
                }
                log.warn("ClientHubAI notification batch failed (attempt {}/3); retrying. cause={}",
                        attempt, ex.getMessage());
                try {
                    Thread.sleep(2_000L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                    throw ex;
                }
            }
        }
        throw lastFailure;
    }

    private boolean isTransientDbFailure(Throwable error) {
        Throwable current = error;
        while (current != null) {
            if (current instanceof org.springframework.dao.DataAccessResourceFailureException
                    || current instanceof org.springframework.dao.TransientDataAccessException
                    || current instanceof java.net.SocketException
                    || current instanceof java.io.IOException) {
                return true;
            }
            String message = current.getMessage();
            if (message != null) {
                String lower = message.toLowerCase(Locale.ROOT);
                if (lower.contains("i/o error")
                        || lower.contains("connection has been closed")
                        || lower.contains("connection reset")
                        || lower.contains("broken pipe")
                        || lower.contains("terminating connection")
                        || lower.contains("socket closed")
                        || lower.contains("admin shutdown")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private Counts executeNotificationBatch(
            List<SourceNotificationRecord> notifications,
            TargetInventory target,
            Map<String, Long> userMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings) {
        Counts counts = new Counts();
        if (notifications.isEmpty()) {
            return counts;
        }

        String table = ClientHubIdentifier.qualified(target.schemaName(), "notifications");
        StringBuilder sql = new StringBuilder("""
                INSERT INTO %s (
                    createdat, created_by, updatedat, updated_by, version, is_deleted, deleted_at,
                    user_id, client_id, type, category, title, message, data, priority,
                    is_read, read_at, action_url, action_label, grouping_key, expires_at,
                    related_entity_type, related_entity_id, email_sent, email_sent_at, email_error
                ) VALUES
                """.formatted(table));
        List<Object> args = new ArrayList<>(notifications.size() * 20);
        for (int i = 0; i < notifications.size(); i++) {
            if (i > 0) {
                sql.append(',');
            }
            sql.append("(?,?,?,?,?,FALSE,NULL,?,NULL,?,?,?,?,?,?,?,?,?,?,?,?,?,?,FALSE,NULL,NULL)");
            SourceNotificationRecord source = notifications.get(i);
            appendNotificationInsertArgs(args, source, userMappings, clientMappings, sessionMappings,
                    taskMappings, documentMappings);
        }
        sql.append(" RETURNING id");

        List<Long> generatedIds = jdbcTemplate.query(sql.toString(), (rs, rowNum) -> rs.getLong(1), args.toArray());
        if (generatedIds.size() != notifications.size()) {
            throw new IllegalStateException("Notification insert returned " + generatedIds.size()
                    + " ids for " + notifications.size() + " source rows");
        }

        List<Object[]> mappingRows = new ArrayList<>(notifications.size());
        for (int i = 0; i < notifications.size(); i++) {
            SourceNotificationRecord source = notifications.get(i);
            Long targetId = generatedIds.get(i);
            mappingRows.add(new Object[]{
                    target.organisationId(),
                    source.legacyNotificationPk(),
                    target.schemaName(),
                    "notifications",
                    targetId,
                    notificationChecksum(source)
            });
            counts.created++;
        }
        batchUpsertLegacyMappings(mappingRows, "notifications");
        return counts;
    }

    private void appendNotificationInsertArgs(
            List<Object> args,
            SourceNotificationRecord source,
            Map<String, Long> userMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings) {
        NotificationType type = ClientHubNotificationTypeMapper.mapTypeOrFallback(source.type());
        boolean fallbackType = ClientHubNotificationTypeMapper.mapType(source.type()).isEmpty();
        Instant createdAt = source.createdAt() == null ? Instant.now() : source.createdAt();
        Long userId = requiredMapping(userMappings, source.userLegacyId(), "users");
        Long relatedId = resolveRelatedEntityId(
                source.relatedEntityType(),
                source.relatedEntityLegacyId(),
                clientMappings,
                sessionMappings,
                taskMappings,
                documentMappings,
                userMappings);

        args.add(Timestamp.from(createdAt));
        args.add(0L);
        args.add(Timestamp.from(createdAt));
        args.add(0L);
        args.add(0L);
        args.add(userId);
        args.add(type.name());
        args.add(type.getCategory().name());
        args.add(source.title().trim());
        args.add(source.message());
        args.add(mergeLegacyType(source.data(), source.type(), fallbackType));
        args.add(ClientHubNotificationTypeMapper.mapPriority(source.priority()).name());
        args.add(source.read());
        args.add(source.readAt() == null ? null : Timestamp.from(source.readAt()));
        args.add(trim(source.actionUrl()));
        args.add(trim(source.actionLabel()));
        args.add(trim(source.groupingKey()));
        args.add(source.expiresAt() == null ? null : Timestamp.from(source.expiresAt()));
        args.add(trim(source.relatedEntityType()));
        args.add(relatedId);
    }

    private void batchUpsertLegacyMappings(List<Object[]> rows, String entityName) {
        if (rows.isEmpty()) {
            return;
        }
        jdbcTemplate.batchUpdate("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', '%s', ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """.formatted(entityName), rows);
    }

    private Set<String> loadMappedSourceIdSet(Long organisationId, String entityName) {
        return Set.copyOf(jdbcTemplate.query("""
                SELECT source_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (rs, rowNum) -> rs.getString("source_id"),
                organisationId,
                entityName));
    }

    private Counts executeScheduledBatch(
            List<SourceScheduledNotificationRecord> scheduled,
            TargetInventory target,
            Map<String, Long> triggerMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings,
            Map<String, Long> userMappings) {
        Map<String, Long> liveTriggerMappings = loadMappings(target.organisationId(), "notification_triggers");
        liveTriggerMappings.putAll(triggerMappings);
        Map<String, Long> liveSessionMappings = loadMappings(target.organisationId(), "sessions");
        liveSessionMappings.putAll(sessionMappings);
        Map<String, Long> scheduledMappings = loadMappings(target.organisationId(), "scheduled_notifications");

        Counts counts = new Counts();
        for (SourceScheduledNotificationRecord source : scheduled) {
            if (source.sessionLegacyId() != null && !liveSessionMappings.containsKey(source.sessionLegacyId())) {
                counts.skipped++;
                continue;
            }
            Optional<Long> mappedId = Optional.ofNullable(scheduledMappings.get(source.legacyScheduledPk()));
            ScheduledNotification row = mappedId.flatMap(scheduledNotificationRepository::findById)
                    .orElseGet(ScheduledNotification::new);
            boolean existing = row.getId() != null;
            applyScheduledFields(row, source, liveTriggerMappings, liveSessionMappings, clientMappings, taskMappings,
                    documentMappings, userMappings);
            ScheduledNotification saved = scheduledNotificationRepository.save(row);
            scheduledMappings.put(source.legacyScheduledPk(), saved.getId());
            upsertLegacyMapping(target, "scheduled_notifications", "scheduled_notifications",
                    source.legacyScheduledPk(), saved.getId(), scheduledChecksum(source));
            if (existing) {
                counts.updated++;
                if (mappedId.isPresent()) {
                    counts.mappedReruns++;
                }
            } else {
                counts.created++;
            }
        }
        return counts;
    }

    private void applyTemplateFields(NotificationTemplate template, SourceNotificationTemplateRecord source) {
        template.setName(source.name().trim());
        template.setType(source.type().trim());
        template.setSubject(source.subject().trim());
        template.setBodyTemplate(source.bodyTemplate());
        template.setActionUrlTemplate(trim(source.actionUrlTemplate()));
        template.setActionLabel(trim(source.actionLabel()));
        template.setRecipientRoles(trim(source.recipientRoles()));
        template.setVariables(trim(source.variables()));
        template.setIsSystem(source.system());
        template.setIsActive(source.active());
        template.setIsDeleted(false);
        template.setDeletedAt(null);
        stampAudit(template, source.createdAt(), source.updatedAt());
    }

    private void applyTriggerFields(
            NotificationTrigger trigger,
            SourceNotificationTriggerRecord source,
            Map<String, Long> templateMappings) {
        trigger.setName(source.name().trim());
        trigger.setDescription(trim(source.description()));
        trigger.setEventType(source.eventType().trim());
        trigger.setEntityType(mapEntityType(source.entityType()));
        trigger.setConditionRules(trim(source.conditionRules()));
        trigger.setRecipientRules(trim(source.recipientRules()));
        if (source.templateLegacyId() == null) {
            trigger.setTemplate(null);
        } else {
            trigger.setTemplate(templateRepository.getReferenceById(
                    requiredMapping(templateMappings, source.templateLegacyId(), "notification_templates")));
        }
        trigger.setPriority(normaliseLower(source.priority(), "medium"));
        trigger.setDelayMinutes(source.delayMinutes() == null ? 0 : source.delayMinutes());
        trigger.setBatchWindowMinutes(source.batchWindowMinutes() == null ? 5 : source.batchWindowMinutes());
        trigger.setMaxBatchSize(source.maxBatchSize() == null ? 10 : source.maxBatchSize());
        trigger.setIsScheduled(source.scheduled());
        trigger.setIsActive(source.active());
        trigger.setIsDeleted(false);
        trigger.setDeletedAt(null);
        stampAudit(trigger, source.createdAt(), source.updatedAt());
    }

    private void applyPreferenceFields(
            NotificationPreference preference,
            SourceNotificationPreferenceRecord source,
            Map<String, Long> userMappings) {
        NotificationType type = ClientHubNotificationTypeMapper.mapType(source.triggerType())
                .orElseThrow(() -> new IllegalStateException(
                        "Unmapped notification preference trigger_type: " + source.triggerType()));
        preference.setUser(userRepository.getReferenceById(
                requiredMapping(userMappings, source.userLegacyId(), "users")));
        preference.setClient(null);
        preference.setNotificationType(type);
        preference.setInAppEnabled(source.enableInApp());
        preference.setEmailEnabled(source.enableEmail());
        preference.setSmsEnabled(source.enableSms());
        preference.setPushEnabled(false);
        preference.setTiming(ClientHubNotificationTypeMapper.mapTiming(source.timing()));
        preference.setQuietHoursStart(null);
        preference.setQuietHoursEnd(null);
        preference.setWeekendsEnabled(source.weekendsEnabled());
        preference.setIsDeleted(false);
        preference.setDeletedAt(null);
        stampAudit(preference, source.createdAt(), source.updatedAt());
    }

    private void applyScheduledFields(
            ScheduledNotification row,
            SourceScheduledNotificationRecord source,
            Map<String, Long> triggerMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings,
            Map<String, Long> userMappings) {
        row.setTrigger(triggerRepository.getReferenceById(
                requiredMapping(triggerMappings, source.triggerLegacyId(), "notification_triggers")));
        if (source.sessionLegacyId() == null) {
            row.setSession(null);
        } else {
            row.setSession(sessionRepository.getReferenceById(
                    requiredMapping(sessionMappings, source.sessionLegacyId(), "sessions")));
        }
        row.setEntityType(mapEntityType(source.entityType()));
        row.setEntityId(resolveRelatedEntityId(
                source.entityType(),
                source.entityLegacyId(),
                clientMappings,
                sessionMappings,
                taskMappings,
                documentMappings,
                userMappings));
        if (row.getEntityId() == null) {
            // Preserve a numeric entity id when remapping is unavailable so the row stays valid.
            row.setEntityId(Long.valueOf(source.entityLegacyId()));
        }
        row.setEntityData(source.entityData());
        row.setExecuteAt(source.executeAt());
        row.setStatus(ClientHubNotificationTypeMapper.mapScheduledStatus(source.status()));
        row.setRetryCount(source.retryCount() == null ? 0 : source.retryCount());
        row.setLastError(trim(source.lastError()));
        row.setProcessedAt(source.processedAt());
        row.setIsDeleted(false);
        row.setDeletedAt(null);
        stampAudit(row, source.createdAt(), source.processedAt() == null ? source.createdAt() : source.processedAt());
    }

    private Long resolveRelatedEntityId(
            String entityType,
            String legacyId,
            Map<String, Long> clientMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings,
            Map<String, Long> userMappings) {
        if (legacyId == null || legacyId.isBlank()) {
            return null;
        }
        String normalised = entityType == null ? "" : entityType.trim().toLowerCase(Locale.ROOT);
        return switch (normalised) {
            case "client", "clients" -> clientMappings.get(legacyId);
            case "session", "sessions" -> sessionMappings.get(legacyId);
            case "task", "tasks" -> taskMappings.get(legacyId);
            case "document", "documents" -> documentMappings.get(legacyId);
            case "user", "users" -> userMappings.get(legacyId);
            default -> null;
        };
    }

    private EntityType mapEntityType(String raw) {
        if (raw == null || raw.isBlank()) {
            return EntityType.GENERAL;
        }
        String normalised = raw.trim().toUpperCase(Locale.ROOT);
        try {
            return EntityType.valueOf(normalised);
        } catch (IllegalArgumentException ex) {
            return switch (raw.trim().toLowerCase(Locale.ROOT)) {
                case "client", "clients" -> EntityType.CLIENT;
                case "session", "sessions" -> EntityType.SESSION;
                case "task", "tasks" -> EntityType.TASK;
                case "document", "documents" -> EntityType.DOCUMENT;
                case "billing", "payment", "invoice" -> EntityType.BILLING;
                case "form", "forms" -> EntityType.FORM;
                case "user", "users" -> EntityType.USER;
                case "assessment", "assessments" -> EntityType.ASSESSMENT;
                case "checklist", "checklists" -> EntityType.CHECKLIST;
                default -> EntityType.GENERAL;
            };
        }
    }

    private String mergeLegacyType(String data, String legacyType, boolean fallbackType) {
        if (!fallbackType) {
            return trim(data);
        }
        String marker = "\"_legacyType\":\"" + legacyType.replace("\"", "\\\"") + "\"";
        if (data == null || data.isBlank()) {
            return "{" + marker + "}";
        }
        String trimmed = data.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            if (trimmed.length() == 2) {
                return "{" + marker + "}";
            }
            return trimmed.substring(0, trimmed.length() - 1) + "," + marker + "}";
        }
        return "{\"_payload\":" + quoteJson(trimmed) + "," + marker + "}";
    }

    private String quoteJson(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private Map<String, Long> loadTemplateIdsByName() {
        Map<String, Long> byName = new HashMap<>();
        for (NotificationTemplate template : templateRepository.findAll()) {
            if (template.getName() != null && !template.getName().isBlank() && template.getId() != null) {
                byName.putIfAbsent(template.getName(), template.getId());
            }
        }
        return byName;
    }

    private Map<String, Long> loadTriggerIdsByName() {
        Map<String, Long> byName = new HashMap<>();
        for (NotificationTrigger trigger : triggerRepository.findAll()) {
            if (trigger.getName() != null && !trigger.getName().isBlank() && trigger.getId() != null) {
                byName.putIfAbsent(trigger.getName(), trigger.getId());
            }
        }
        return byName;
    }

    private Map<String, Long> loadMappings(Long organisationId, String entityName) {
        Map<String, Long> mappings = new HashMap<>();
        jdbcTemplate.query("""
                SELECT source_id, target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = ?
                """,
                (RowCallbackHandler) rs -> mappings.put(rs.getString("source_id"), rs.getLong("target_id")),
                organisationId,
                entityName);
        return mappings;
    }

    private void upsertLegacyMapping(
            TargetInventory target,
            String entityName,
            String targetTable,
            String sourceId,
            Long targetId,
            String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', ?, ?, ?, ?, ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                entityName,
                sourceId,
                target.schemaName(),
                targetTable,
                targetId,
                checksum);
    }

    private Long requiredMapping(Map<String, Long> mappings, String sourceId, String entityName) {
        Long targetId = mappings.get(sourceId);
        if (targetId == null) {
            throw new IllegalStateException("Missing ClientHubAI " + entityName + " mapping for source id " + sourceId);
        }
        return targetId;
    }

    private void stampAudit(
            com.smart.therapy.flow.common.entity.BaseEntity entity,
            java.time.Instant createdAt,
            java.time.Instant updatedAt) {
        if (createdAt != null) {
            entity.setCreatedAt(createdAt);
        }
        if (updatedAt != null) {
            entity.setUpdatedAt(updatedAt);
        }
        entity.setCreatedBy(0L);
        entity.setUpdatedBy(0L);
    }

    private String templateChecksum(SourceNotificationTemplateRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTemplatePk(),
                source.name(),
                source.type(),
                source.subject(),
                Boolean.toString(source.active())));
    }

    private String triggerChecksum(SourceNotificationTriggerRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTriggerPk(),
                source.name(),
                source.eventType(),
                source.entityType(),
                String.valueOf(source.templateLegacyId()),
                Boolean.toString(source.active())));
    }

    private String preferenceChecksum(SourceNotificationPreferenceRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyPreferencePk(),
                source.userLegacyId(),
                source.triggerType(),
                source.timing(),
                Boolean.toString(source.enableInApp()),
                Boolean.toString(source.enableEmail()),
                Boolean.toString(source.enableSms())));
    }

    private String notificationChecksum(SourceNotificationRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyNotificationPk(),
                source.userLegacyId(),
                source.type(),
                source.title(),
                Boolean.toString(source.read()),
                String.valueOf(source.relatedEntityType()),
                String.valueOf(source.relatedEntityLegacyId())));
    }

    private String scheduledChecksum(SourceScheduledNotificationRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyScheduledPk(),
                source.triggerLegacyId(),
                String.valueOf(source.sessionLegacyId()),
                source.entityType(),
                source.entityLegacyId(),
                String.valueOf(source.executeAt()),
                String.valueOf(source.status())));
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String normaliseLower(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record NotificationExecuteResult(
            int sourceTemplates,
            int templatesCreated,
            int templatesUpdated,
            int templateMappedReruns,
            int sourceTriggers,
            int triggersCreated,
            int triggersUpdated,
            int triggerMappedReruns,
            int sourcePreferences,
            int preferencesCreated,
            int preferencesUpdated,
            int preferenceMappedReruns,
            int preferencesSkipped,
            int sourceNotifications,
            int notificationsCreated,
            int notificationsUpdated,
            int notificationMappedReruns,
            int sourceScheduled,
            int scheduledCreated,
            int scheduledUpdated,
            int scheduledMappedReruns,
            int scheduledSkipped) {
    }

    private record ConfigBootstrap(
            Map<String, Long> userMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> sessionMappings,
            Map<String, Long> taskMappings,
            Map<String, Long> documentMappings,
            Map<String, Long> triggerMappings,
            int templatesCreated,
            int templatesUpdated,
            int templateMappedReruns,
            int triggersCreated,
            int triggersUpdated,
            int triggerMappedReruns,
            int preferencesCreated,
            int preferencesUpdated,
            int preferenceMappedReruns,
            int preferencesSkipped) {
    }

    private static final class Counts {
        int created;
        int updated;
        int mappedReruns;
        int skipped;

        void add(Counts other) {
            created += other.created;
            updated += other.updated;
            mappedReruns += other.mappedReruns;
            skipped += other.skipped;
        }
    }
}
