package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.client.repository.ClientRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceTranscriptRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import com.smart.therapy.flow.session.entity.SessionTranscript;
import com.smart.therapy.flow.session.enums.SessionTranscriptStatus;
import com.smart.therapy.flow.session.repository.SessionRepository;
import com.smart.therapy.flow.session.repository.SessionTranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientHubTranscriptsExecuteService {

    private static final int BATCH_SIZE = 25;

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final SessionTranscriptRepository sessionTranscriptRepository;
    private final SessionRepository sessionRepository;
    private final ClientRepository clientRepository;
    private final UserRepository userRepository;

    public TranscriptsExecuteResult execute(List<SourceTranscriptRecord> transcripts, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException(
                    "Exactly one target organisation is required for transcripts execution");
        }

        Map<String, Long> sessionMappings = loadMappings(target.organisationId(), "sessions");
        Map<String, Long> clientMappings = loadMappings(target.organisationId(), "clients");
        Map<String, Long> userMappings = loadMappings(target.organisationId(), "users");

        Counts total = runBatched(transcripts, batch -> tenantTransactionExecutor.executeWrite(
                target.organisationId(),
                target.schemaName(),
                () -> upsertBatch(batch, target, sessionMappings, clientMappings, userMappings)));

        return new TranscriptsExecuteResult(
                transcripts.size(),
                total.created,
                total.updated,
                total.mappedReruns);
    }

    private Counts upsertBatch(
            List<SourceTranscriptRecord> batch,
            TargetInventory target,
            Map<String, Long> sessionMappings,
            Map<String, Long> clientMappings,
            Map<String, Long> userMappings) {
        Map<String, Long> transcriptMappings = loadMappings(target.organisationId(), "session_transcripts");
        Counts counts = new Counts();
        for (SourceTranscriptRecord source : batch) {
            if (transcriptMappings.containsKey(source.legacyTranscriptPk())) {
                counts.record(true, true);
                continue;
            }

            Long sessionId = requiredMapping(sessionMappings, source.sessionLegacyId(), "sessions");
            Long clientId = requiredMapping(clientMappings, source.clientLegacyId(), "clients");
            Long therapistId = requiredMapping(userMappings, source.therapistLegacyId(), "users");

            SessionTranscriptStatus status = mapStatus(source.status());
            Instant createdAt = source.createdAt() != null ? source.createdAt() : Instant.now();
            Instant updatedAt = source.updatedAt() != null ? source.updatedAt() : createdAt;

            SessionTranscript transcript = SessionTranscript.builder()
                    .session(sessionRepository.getReferenceById(sessionId))
                    .client(clientRepository.getReferenceById(clientId))
                    .uploader(userRepository.getReferenceById(therapistId))
                    .uploadId(resolveUploadId(source))
                    .status(status)
                    .language(trimTo(source.language(), 20))
                    .translatedToEnglish(source.translatedToEnglish())
                    .expectedChunks(source.chunkCount())
                    .receivedChunks(source.chunkCount() != null ? source.chunkCount() : 0)
                    .durationSeconds(source.durationSeconds())
                    .wordCount(source.wordCount())
                    .finalTranscript(trim(source.content()))
                    .rawContent(trim(source.rawContent()))
                    .failureReason(trim(source.errorMessage()))
                    .finalizedAt(status == SessionTranscriptStatus.READY ? updatedAt : null)
                    .expiresAt(createdAt.plus(3650, ChronoUnit.DAYS))
                    .build();
            transcript.setIsDeleted(false);
            transcript.setDeletedAt(null);
            transcript.setCreatedBy(0L);
            transcript.setUpdatedBy(0L);
            if (source.createdAt() != null) {
                transcript.setCreatedAt(source.createdAt());
            }
            if (source.updatedAt() != null) {
                transcript.setUpdatedAt(source.updatedAt());
            }

            SessionTranscript saved = sessionTranscriptRepository.save(transcript);
            transcriptMappings.put(source.legacyTranscriptPk(), saved.getId());
            upsertLegacyMapping(target, source.legacyTranscriptPk(), saved.getId(), transcriptChecksum(source));
            counts.record(false, false);
        }
        return counts;
    }

    private Counts runBatched(
            List<SourceTranscriptRecord> rows,
            java.util.function.Function<List<SourceTranscriptRecord>, Counts> worker) {
        Counts total = new Counts();
        if (rows.isEmpty()) {
            return total;
        }
        int batchNumber = 0;
        for (int start = 0; start < rows.size(); start += BATCH_SIZE) {
            int end = Math.min(start + BATCH_SIZE, rows.size());
            int currentBatch = ++batchNumber;
            List<SourceTranscriptRecord> batch = rows.subList(start, end);
            Counts batchCounts = runBatchWithRetry(currentBatch, batch, worker);
            total.add(batchCounts);
            log.info("ClientHubAI transcripts batch complete: batch={} batch_size={} processed={}/{}",
                    currentBatch, batch.size(), end, rows.size());
        }
        return total;
    }

    private Counts runBatchWithRetry(
            int batchNumber,
            List<SourceTranscriptRecord> batch,
            java.util.function.Function<List<SourceTranscriptRecord>, Counts> worker) {
        RuntimeException lastFailure = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                return worker.apply(batch);
            } catch (RuntimeException ex) {
                lastFailure = ex;
                if (attempt >= 3 || !isTransientDbFailure(ex)) {
                    throw ex;
                }
                log.warn("ClientHubAI transcripts batch failed (batch={} attempt={}/3); retrying. cause={}",
                        batchNumber, attempt, ex.getMessage());
                try {
                    Thread.sleep(3_000L * attempt);
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
                    || current instanceof java.net.SocketTimeoutException
                    || current instanceof java.io.IOException
                    || (current.getMessage() != null && (
                    current.getMessage().contains("I/O error")
                            || current.getMessage().contains("Connection is closed")
                            || current.getMessage().contains("This connection has been closed")))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    static SessionTranscriptStatus mapStatus(String status) {
        if (status == null || status.isBlank()) {
            return SessionTranscriptStatus.READY;
        }
        try {
            return SessionTranscriptStatus.fromValue(status);
        } catch (IllegalArgumentException ex) {
            String normalized = status.trim().toLowerCase(Locale.ROOT);
            if (normalized.contains("ready")
                    || normalized.contains("complete")
                    || normalized.contains("done")
                    || normalized.contains("success")) {
                return SessionTranscriptStatus.READY;
            }
            if (normalized.contains("fail") || normalized.contains("error")) {
                return SessionTranscriptStatus.FAILED;
            }
            if (normalized.contains("process") || normalized.contains("finaliz")) {
                return SessionTranscriptStatus.PROCESSING;
            }
            if (normalized.contains("record") || normalized.contains("upload")) {
                return SessionTranscriptStatus.RECORDING;
            }
            return SessionTranscriptStatus.READY;
        }
    }

    static String resolveUploadId(SourceTranscriptRecord source) {
        if (source.uploadId() != null && !source.uploadId().isBlank()) {
            return source.uploadId().trim();
        }
        return "ch-transcript-" + source.legacyTranscriptPk();
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

    private void upsertLegacyMapping(TargetInventory target, String sourceId, Long targetId, String checksum) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', 'session_transcripts', ?, ?, 'session_transcripts', ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                sourceId,
                target.schemaName(),
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

    private String transcriptChecksum(SourceTranscriptRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyTranscriptPk(),
                nullToEmpty(source.sessionLegacyId()),
                nullToEmpty(source.clientLegacyId()),
                nullToEmpty(source.status()),
                nullToEmpty(source.uploadId()),
                String.valueOf(source.wordCount())));
    }

    private static String trim(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String trimTo(String value, int maxLength) {
        String trimmed = trim(value);
        if (trimmed == null) {
            return null;
        }
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static final class Counts {
        int created;
        int updated;
        int mappedReruns;

        void record(boolean existing, boolean mappedRerun) {
            if (mappedRerun) {
                mappedReruns++;
            } else if (existing) {
                updated++;
            } else {
                created++;
            }
        }

        void add(Counts other) {
            created += other.created;
            updated += other.updated;
            mappedReruns += other.mappedReruns;
        }
    }

    record TranscriptsExecuteResult(
            int sourceTranscripts,
            int created,
            int updated,
            int mappedReruns) {
    }
}
