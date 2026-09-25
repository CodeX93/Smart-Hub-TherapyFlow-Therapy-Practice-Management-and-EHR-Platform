package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionNoteRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceSessionRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.session.entity.Room;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.entity.SessionNote;
import com.smart.therapy.flow.session.enums.RoomType;
import com.smart.therapy.flow.session.repository.SessionNoteRepository;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubSessionExecuteService {

    private static final int BATCH_SIZE = 100;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final SessionRepository sessionRepository;
    private final SessionNoteRepository sessionNoteRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;
    private final ServiceRepository serviceRepository;
    private final RoomRepository roomRepository;

    public SessionExecuteResult execute(
            List<SourceSessionRecord> sourceSessions,
            List<SourceSessionNoteRecord> sourceNotes,
            TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for session execution");
        }

        SessionExecuteResult total = SessionExecuteResult.empty();
        int sessionBatchNumber = 0;
        for (int start = 0; start < sourceSessions.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourceSessions.size());
            int currentBatch = ++sessionBatchNumber;
            List<SourceSessionRecord> batch = sourceSessions.subList(start, end);
            SessionExecuteResult batchResult = tenantTransactionExecutor.executeWrite(
                    target.organisationId(), target.schemaName(),
                    () -> executeSessionBatch(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI session execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatch, batch.size(), total.sourceSessions(), sourceSessions.size());
        }

        int noteBatchNumber = 0;
        for (int start = 0; start < sourceNotes.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, sourceNotes.size());
            int currentBatch = ++noteBatchNumber;
            List<SourceSessionNoteRecord> batch = sourceNotes.subList(start, end);
            SessionExecuteResult batchResult = tenantTransactionExecutor.executeWrite(
                    target.organisationId(), target.schemaName(),
                    () -> executeNoteBatch(batch, target));
            total = total.plus(batchResult);
            log.info("ClientHubAI session-note execute batch complete: batch={} batch_size={} processed={} total={}",
                    currentBatch, batch.size(), total.sourceNotes(), sourceNotes.size());
        }

        return total;
    }

    private SessionExecuteResult executeSessionBatch(List<SourceSessionRecord> sourceSessions, TargetInventory target) {
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> serviceMappings = loadMappings(target.organisationId(), "services");
        Map<String, Long> roomMappings = loadMappings(target.organisationId(), "rooms");
        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");

        int sessionsCreated = 0;
        int sessionsUpdated = 0;
        int sessionMappedReruns = 0;
        for (SourceSessionRecord source : sourceSessions) {
            Optional<Long> mappedSessionId = Optional.ofNullable(sessionMappings.get(source.legacySessionPk()));
            Session session = mappedSessionId.flatMap(sessionRepository::findById).orElseGet(Session::new);
            boolean existing = session.getId() != null;

            applySessionFields(session, source, clientMappings, userMappings, serviceMappings, roomMappings);
            Session saved = sessionRepository.save(session);
            sessionMappings.put(source.legacySessionPk(), saved.getId());
            upsertLegacyMapping(target, "sessions", "sessions", source.legacySessionPk(), saved.getId(), sessionChecksum(source));

            if (existing) {
                sessionsUpdated++;
                if (mappedSessionId.isPresent()) {
                    sessionMappedReruns++;
                }
            } else {
                sessionsCreated++;
            }
        }

        return new SessionExecuteResult(
                sourceSessions.size(), sessionsCreated, sessionsUpdated, sessionMappedReruns,
                0, 0, 0, 0);
    }

    private SessionExecuteResult executeNoteBatch(List<SourceSessionNoteRecord> sourceNotes, TargetInventory target) {
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");
        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");
        Map<String, Long> noteMappings = loadMappings(target.organisationId(), "session_notes");

        int notesCreated = 0;
        int notesUpdated = 0;
        int noteMappedReruns = 0;
        for (SourceSessionNoteRecord source : sourceNotes) {
            if (!sessionMappings.containsKey(source.sessionLegacyId())) {
                throw new IllegalStateException(
                        "Missing ClientHubAI sessions mapping for source id " + source.sessionLegacyId());
            }

            Optional<Long> mappedNoteId = Optional.ofNullable(noteMappings.get(source.legacyNotePk()));
            SessionNote note = mappedNoteId.flatMap(sessionNoteRepository::findById).orElseGet(SessionNote::new);
            boolean existing = note.getId() != null;

            // Target DB trigger rejects UPDATE/DELETE when is_finalized=true. Keep mapping
            // checksum current and skip content rewrite so migration/sync re-runs do not fail.
            if (existing && Boolean.TRUE.equals(note.getIsFinalized())) {
                upsertLegacyMapping(target, "session_notes", "session_notes",
                        source.legacyNotePk(), note.getId(), noteChecksum(source));
                notesUpdated++;
                if (mappedNoteId.isPresent()) {
                    noteMappedReruns++;
                }
                log.debug("ClientHubAI session note skip finalized update: legacy_id={} target_id={}",
                        source.legacyNotePk(), note.getId());
                continue;
            }

            applyNoteFields(note, source, clientMappings, userMappings, sessionMappings);
            SessionNote saved = sessionNoteRepository.save(note);
            noteMappings.put(source.legacyNotePk(), saved.getId());
            upsertLegacyMapping(target, "session_notes", "session_notes",
                    source.legacyNotePk(), saved.getId(), noteChecksum(source));

            if (existing) {
                notesUpdated++;
                if (mappedNoteId.isPresent()) {
                    noteMappedReruns++;
                }
            } else {
                notesCreated++;
            }
        }

        return new SessionExecuteResult(0, 0, 0, 0, sourceNotes.size(), notesCreated, notesUpdated, noteMappedReruns);
    }

    private void applySessionFields(
            Session session,
            SourceSessionRecord source,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings,
            Map<String, Long> serviceMappings,
            Map<String, Long> roomMappings) {
        session.setClient(clientRepository.getReferenceById(requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
        session.setTherapist(userRepository.getReferenceById(requiredMapping(userMappings, source.therapistLegacyId(), "users")));
        session.setService(serviceRepository.getReferenceById(requiredMapping(serviceMappings, source.serviceLegacyId(), "services")));
        if (source.roomLegacyId() != null && roomMappings.containsKey(source.roomLegacyId())) {
            Room room = roomRepository.findById(roomMappings.get(source.roomLegacyId())).orElse(null);
            session.setRoom(room);
        } else {
            session.setRoom(null);
        }
        session.setSessionDate(source.sessionDate());
        session.setClinicalSessionType(defaultIfBlank(source.sessionType(), "General"));
        session.setSessionType(sessionMode(source, session.getRoom()));
        session.setStatus(normalizeStatus(source.status()));
        session.setDuration(source.duration());
        session.setNotes(trim(source.notes()));
        session.setCalculatedRate(source.calculatedRate());
        session.setInsuranceApplicable(source.insuranceApplicable());
        session.setBillingNotes(trim(source.billingNotes()));
        session.setRecurrenceGroupId(trimTo(source.recurrenceGroupId(), 64));
        session.setIsDeleted(false);
        session.setDeletedAt(null);
        session.setCreatedBy(0L);
        session.setUpdatedBy(0L);
    }

    private void applyNoteFields(
            SessionNote note,
            SourceSessionNoteRecord source,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings,
            Map<String, Long> sessionMappings) {
        note.setSession(sessionRepository.getReferenceById(requiredMapping(sessionMappings, source.sessionLegacyId(), "sessions")));
        note.setClient(clientRepository.getReferenceById(requiredMapping(clientMappings, source.clientLegacyId(), "clients")));
        note.setTherapist(userRepository.getReferenceById(requiredMapping(userMappings, source.therapistLegacyId(), "users")));
        note.setDate(source.noteDate());
        note.setSessionFocus(trim(source.sessionFocus()));
        note.setSymptoms(trim(source.symptoms()));
        note.setShortTermGoals(trim(source.shortTermGoals()));
        note.setIntervention(trim(source.intervention()));
        note.setProgress(trim(source.progress()));
        note.setRemarks(trim(source.remarks()));
        note.setRecommendations(trim(source.recommendations()));
        note.setClientRating(source.clientRating());
        note.setTherapistRating(source.therapistRating());
        note.setProgressTowardGoals(source.progressTowardGoals());
        note.setMoodBefore(source.moodBefore());
        note.setMoodAfter(source.moodAfter());
        note.setRiskSuicidalIdeation(source.riskSuicidalIdeation());
        note.setRiskSelfHarm(source.riskSelfHarm());
        note.setRiskHomicidalIdeation(source.riskHomicidalIdeation());
        note.setRiskPsychosis(source.riskPsychosis());
        note.setRiskSubstanceUse(source.riskSubstanceUse());
        note.setRiskImpulsivity(source.riskImpulsivity());
        note.setRiskAggression(source.riskAggression());
        note.setRiskTraumaSymptoms(source.riskTraumaSymptoms());
        note.setRiskNonAdherence(source.riskNonAdherence());
        note.setRiskSupportSystem(source.riskSupportSystem());
        note.setGeneratedContent(trim(source.generatedContent()));
        note.setDraftContent(trim(source.draftContent()));
        note.setFinalContent(trim(source.finalContent()));
        note.setIsDraft(source.draft());
        note.setIsFinalized(source.finalized());
        note.setFinalizedAt(source.finalizedAt());
        note.setAiEnabled(source.aiEnabled());
        note.setCustomAiPrompt(trim(source.customAiPrompt()));
        note.setAiProcessingStatus(defaultIfBlank(source.aiProcessingStatus(), "idle"));
        note.setVoiceTranscription(trim(source.voiceTranscription()));
        note.setIsDeleted(false);
        note.setDeletedAt(null);
        note.setCreatedBy(0L);
        note.setUpdatedBy(0L);
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

    private String sessionMode(SourceSessionRecord source, Room room) {
        String value = source.sessionType() == null ? "" : source.sessionType().toLowerCase(Locale.ROOT);
        if (source.zoomEnabled()
                || value.contains("online")
                || value.contains("virtual")
                || value.contains("telehealth")
                || (room != null && room.getRoomType() == RoomType.VIRTUAL)) {
            return "online";
        }
        return "in-person";
    }

    private String normalizeStatus(String status) {
        return defaultIfBlank(status, "scheduled").trim().toLowerCase(Locale.ROOT).replace('_', '-');
    }

    private String sessionChecksum(SourceSessionRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacySessionPk(),
                source.clientLegacyId(),
                source.therapistLegacyId(),
                source.serviceLegacyId(),
                source.sessionDate().toString(),
                defaultIfBlank(source.sessionType(), ""),
                normalizeStatus(source.status())));
    }

    private String noteChecksum(SourceSessionNoteRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyNotePk(),
                source.sessionLegacyId(),
                source.clientLegacyId(),
                source.therapistLegacyId(),
                source.noteDate().toString(),
                Boolean.toString(source.draft()),
                Boolean.toString(source.finalized())));
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    private String trimTo(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record SessionExecuteResult(
            int sourceSessions,
            int sessionsCreated,
            int sessionsUpdated,
            int sessionMappedReruns,
            int sourceNotes,
            int notesCreated,
            int notesUpdated,
            int noteMappedReruns) {

        static SessionExecuteResult empty() {
            return new SessionExecuteResult(0, 0, 0, 0, 0, 0, 0, 0);
        }

        SessionExecuteResult plus(SessionExecuteResult other) {
            return new SessionExecuteResult(
                    sourceSessions + other.sourceSessions,
                    sessionsCreated + other.sessionsCreated,
                    sessionsUpdated + other.sessionsUpdated,
                    sessionMappedReruns + other.sessionMappedReruns,
                    sourceNotes + other.sourceNotes,
                    notesCreated + other.notesCreated,
                    notesUpdated + other.notesUpdated,
                    noteMappedReruns + other.noteMappedReruns);
        }
    }
}
