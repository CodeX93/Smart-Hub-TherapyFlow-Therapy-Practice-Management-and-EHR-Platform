package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.common.entity.PlatformPhiBlindIndexBackfillJob;
import com.smart.therapy.flow.common.repository.PlatformPhiBlindIndexBackfillJobRepository;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.security.MessageDigest;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Backfills Approach C blind-index digests and optional MRN / v2d→v2 re-encrypt cutover.
 * Enable with {@code app.phi-encryption.blind-index-backfill.enabled=true}.
 * Only schemas that already have {@code client_id_blind_idx} (V56+) are seeded.
 * Cutover order: CLIENT_DIGESTS → CONTACT_DIGESTS → ENCRYPT_MRN → REENCRYPT_*_V2,
 * then set {@code APP_PHI_ENCRYPTION_SEARCH_MODE=blind_only}.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PhiBlindIndexBackfillScheduler {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z0-9_]+");

    private static final List<String> JOB_KINDS = List.of(
            PlatformPhiBlindIndexBackfillJob.KIND_CLIENT_DIGESTS,
            PlatformPhiBlindIndexBackfillJob.KIND_CONTACT_DIGESTS,
            PlatformPhiBlindIndexBackfillJob.KIND_NAME_TOKEN_DIGESTS,
            PlatformPhiBlindIndexBackfillJob.KIND_NAME_PREFIX_DIGESTS,
            PlatformPhiBlindIndexBackfillJob.KIND_ENCRYPT_MRN,
            PlatformPhiBlindIndexBackfillJob.KIND_REENCRYPT_NAME_V2,
            PlatformPhiBlindIndexBackfillJob.KIND_REENCRYPT_CONTACT_V2
    );

    private final PlatformPhiBlindIndexBackfillJobRepository jobRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final EncryptionService encryptionService;
    private final BlindIndexService blindIndexService;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.phi-encryption.blind-index-backfill.enabled:false}")
    private boolean enabled;

    @Value("${app.phi-encryption.blind-index-backfill.batch-size:50}")
    private int batchSize;

    /** How many backfill job batches to run per scheduler tick (name-token catch-up). */
    @Value("${app.phi-encryption.blind-index-backfill.bursts-per-tick:10}")
    private int nameTokenBurstsPerTick;

    /** Comma-separated schema names; empty = all V56+ schemas. */
    @Value("${app.phi-encryption.blind-index-backfill.schema-allow-list:}")
    private String schemaAllowList;

    @Scheduled(fixedDelayString = "${app.phi-encryption.blind-index-backfill.fixed-delay-ms:60000}")
    public void runBackfillTick() {
        if (!enabled) {
            return;
        }
        try {
            transactionTemplate.executeWithoutResult(status -> seedMissingJobs());
        } catch (Exception ex) {
            log.error("PHI blind-index job seeding failed: {}", ex.getMessage());
            // Don't let seeding failures block job processing
        }
        // Drain several batches per tick so name-token search catch-up is not starved by
        // older CLIENT_DIGESTS / ENCRYPT jobs (one-job-per-minute was leaving tokens empty).
        int bursts = Math.max(1, Math.min(nameTokenBurstsPerTick, 20));
        for (int i = 0; i < bursts; i++) {
            try {
                Boolean ran = transactionTemplate.execute(status -> processOneJob());
                if (!Boolean.TRUE.equals(ran)) {
                    break;
                }
            } catch (Exception ex) {
                log.error("PHI blind-index job processing failed: {}", ex.getMessage());
                // Don't let processing failures propagate to scheduler framework
                break;
            }
        }
    }

    private static boolean isNameIndexJob(String kind) {
        return PlatformPhiBlindIndexBackfillJob.KIND_NAME_TOKEN_DIGESTS.equals(kind)
                || PlatformPhiBlindIndexBackfillJob.KIND_NAME_PREFIX_DIGESTS.equals(kind);
    }

    protected void seedMissingJobs() {
        Instant now = Instant.now();
        Set<String> allow = parseAllowList();
        String currentKeyVersion = computeHmacKeyVersion();

        for (Organisation org : organisationRepository.findAll()) {
            String schema = org.getSchemaName();
            if (schema == null || schema.isBlank() || "public".equalsIgnoreCase(schema)) {
                continue;
            }
            if (!allow.isEmpty() && !allow.contains(schema.toLowerCase(Locale.ROOT))) {
                continue;
            }
            if (!tenantSchemaHealthService.schemaExists(schema)) {
                continue;
            }
            if (!hasBlindIndexColumn(schema)) {
                continue;
            }
            for (String kind : JOB_KINDS) {
                if (isNameIndexJob(kind)
                        && !hasTable(schema, "client_name_blind_indexes")) {
                    continue;
                }
                var existingJob = jobRepository.findBySchemaNameAndJobKind(schema, kind);

                if (existingJob.isPresent()) {
                    PlatformPhiBlindIndexBackfillJob job = existingJob.get();

                    // Auto-reset if HMAC key changed and job is completed
                    if ("COMPLETED".equals(job.getStatus())
                        && currentKeyVersion != null
                        && !currentKeyVersion.equals(job.getHmacKeyVersion())) {
                        log.warn("HMAC key version changed for {}.{} (old={}, new={}) - automatically resetting job for re-indexing",
                                schema, kind,
                                job.getHmacKeyVersion() != null ? job.getHmacKeyVersion().substring(0, 8) : "null",
                                currentKeyVersion.substring(0, 8));
                        resetJobForRerun(job, currentKeyVersion, now);
                        jobRepository.save(job);
                        continue;
                    }

                    // Heal clients that still have no name-token rows after a "completed" job
                    // (create-path gaps, failed batches, or clients added while indexes lagged).
                    // Also reopen when newer client ids exist beyond lastProcessedId — completed
                    // jobs otherwise never revisit clients created after the backfill finished,
                    // and stale/wrong digests on those rows are invisible to "missing token" counts.
                    if ("COMPLETED".equals(job.getStatus())
                            && PlatformPhiBlindIndexBackfillJob.KIND_NAME_PREFIX_DIGESTS.equals(kind)
                            && (countClientsMissingNameTokens(schema) > 0
                                || maxClientId(schema) > job.getLastProcessedId())) {
                        log.warn("Re-opening {}.{} — name-token coverage lag (missingRows or newer client ids)",
                                schema, kind);
                        resetJobForRerun(job, currentKeyVersion, now);
                        jobRepository.save(job);
                    }
                    // CLIENT_DIGESTS / CONTACT_DIGESTS previously only selected NULL digest
                    // columns, so stale non-null digests were never repaired. Reopen when
                    // newer row ids appear beyond lastProcessedId.
                    if ("COMPLETED".equals(job.getStatus())
                            && PlatformPhiBlindIndexBackfillJob.KIND_CLIENT_DIGESTS.equals(kind)
                            && maxClientId(schema) > job.getLastProcessedId()) {
                        log.warn("Re-opening {}.{} — newer client ids beyond lastProcessedId",
                                schema, kind);
                        resetJobForRerun(job, currentKeyVersion, now);
                        jobRepository.save(job);
                    }
                    if ("COMPLETED".equals(job.getStatus())
                            && PlatformPhiBlindIndexBackfillJob.KIND_CONTACT_DIGESTS.equals(kind)
                            && maxContactId(schema) > job.getLastProcessedId()) {
                        log.warn("Re-opening {}.{} — newer contact ids beyond lastProcessedId",
                                schema, kind);
                        resetJobForRerun(job, currentKeyVersion, now);
                        jobRepository.save(job);
                    }
                    continue;
                }

                // Create new job with current key version
                jobRepository.save(PlatformPhiBlindIndexBackfillJob.builder()
                        .schemaName(schema)
                        .organisationId(org.getId())
                        .jobKind(kind)
                        .status("PENDING")
                        .lastProcessedId(0L)
                        .processedCount(0L)
                        .errorCount(0L)
                        .hmacKeyVersion(currentKeyVersion)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        }
    }

    /**
     * @return true if a job was claimed and processed (caller may run another burst)
     */
    protected boolean processOneJob() {
        List<PlatformPhiBlindIndexBackfillJob> runnable = jobRepository.findRunnableJobs();
        if (runnable.isEmpty()) {
            return false;
        }
        PlatformPhiBlindIndexBackfillJob job = runnable.get(0);
        job.setStatus("IN_PROGRESS");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        // Deleted/purged tenants leave PENDING jobs behind; querying missing schemas
        // aborts the shared transaction and starves every other backfill job.
        if (!tenantSchemaHealthService.schemaExists(job.getSchemaName())) {
            job.setStatus("CANCELLED");
            job.setLastError("Cancelled: tenant schema no longer exists");
            job.setUpdatedAt(Instant.now());
            try {
                jobRepository.saveAndFlush(job);
            } catch (Exception saveEx) {
                log.error("Failed to cancel orphan PHI job schema={} kind={}: {}",
                        job.getSchemaName(), job.getJobKind(), saveEx.getMessage());
            }
            log.warn("Cancelled PHI blind-index job for missing schema={} kind={}",
                    job.getSchemaName(), job.getJobKind());
            return true;
        }

        int safeBatch = isNameIndexJob(job.getJobKind())
                ? Math.max(1, Math.min(Math.max(batchSize, 200), 200))
                : Math.max(1, Math.min(batchSize, 100));
        Long previousOrg = TenantContext.getOrganisationId();
        try {
            validateIdentifier(job.getSchemaName());
            TenantContext.setOrganisationId(job.getOrganisationId());

            int processed = switch (job.getJobKind()) {
                case PlatformPhiBlindIndexBackfillJob.KIND_CLIENT_DIGESTS -> backfillClientDigests(job, safeBatch);
                case PlatformPhiBlindIndexBackfillJob.KIND_CONTACT_DIGESTS -> backfillContactDigests(job, safeBatch);
                case PlatformPhiBlindIndexBackfillJob.KIND_NAME_TOKEN_DIGESTS,
                     PlatformPhiBlindIndexBackfillJob.KIND_NAME_PREFIX_DIGESTS -> backfillNameTokenDigests(job, safeBatch);
                case PlatformPhiBlindIndexBackfillJob.KIND_ENCRYPT_MRN -> encryptMrn(job, safeBatch);
                case PlatformPhiBlindIndexBackfillJob.KIND_REENCRYPT_NAME_V2 -> reencryptNameToV2(job, safeBatch);
                case PlatformPhiBlindIndexBackfillJob.KIND_REENCRYPT_CONTACT_V2 -> reencryptContactToV2(job, safeBatch);
                default -> throw new IllegalStateException("Unknown job kind: " + job.getJobKind());
            };

            if (processed == 0) {
                job.setStatus("COMPLETED");
                job.setHmacKeyVersion(computeHmacKeyVersion());
                log.info("PHI blind-index backfill completed: schema={} kind={} hmacKeyVersion={}",
                        job.getSchemaName(), job.getJobKind(),
                        job.getHmacKeyVersion() != null ? job.getHmacKeyVersion().substring(0, 8) : "null");
            } else {
                job.setStatus("PENDING");
                job.setProcessedCount(job.getProcessedCount() + processed);
            }
            job.setLastError(null);
            // Keep incomplete name-token jobs preferred so large tenants are not interleaved away.
            if (isNameIndexJob(job.getJobKind())
                    && !"COMPLETED".equals(job.getStatus())) {
                job.setUpdatedAt(Instant.parse("2018-01-01T00:00:00Z"));
            } else {
                job.setUpdatedAt(Instant.now());
            }
            try {
                jobRepository.saveAndFlush(job);
            } catch (Exception saveEx) {
                log.error("Failed to save PHI blind-index job status for schema={} kind={}: {}",
                        job.getSchemaName(), job.getJobKind(), saveEx.getMessage());
            }
            return true;
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorCount(job.getErrorCount() + 1);
            job.setLastError(ex.getMessage() != null && ex.getMessage().length() > 500
                    ? ex.getMessage().substring(0, 500) : ex.getMessage());
            job.setUpdatedAt(Instant.now());
            try {
                jobRepository.saveAndFlush(job);
            } catch (Exception saveEx) {
                log.error("Failed to save PHI blind-index job failure status for schema={} kind={}: {}",
                        job.getSchemaName(), job.getJobKind(), saveEx.getMessage());
            }
            log.error("PHI blind-index backfill failed schema={} kind={}: {}",
                    job.getSchemaName(), job.getJobKind(), ex.getMessage());
            return true;
        } finally {
            if (previousOrg != null) {
                TenantContext.setOrganisationId(previousOrg);
            } else {
                TenantContext.clear();
            }
        }
    }

    private int backfillClientDigests(PlatformPhiBlindIndexBackfillJob job, int limit) {
        boolean hasDobBlind = hasColumn(job.getSchemaName(), "clients", "date_of_birth_blind_idx");
        // Walk every client by id (not only NULL digests). Stale non-null digests from a
        // wrong HMAC/pepper must be overwritten; NULL-only selection left them permanently broken.
        String sql = hasDobBlind
                ? """
                SELECT id, client_id, full_name, date_of_birth
                FROM %s.clients
                WHERE id > ?
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName())
                : """
                SELECT id, client_id, full_name
                FROM %s.clients
                WHERE id > ?
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        int skipped = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String mrn = asString(row.get("client_id"));
            String fullNameCipher = asString(row.get("full_name"));
            byte[] mrnIdx = null;
            byte[] nameIdx = null;
            byte[] dobIdx = null;
            if (mrn != null && !mrn.isBlank()) {
                String plainMrn = encryptionService.isEncrypted(mrn) ? encryptionService.decrypt(mrn) : mrn;
                mrnIdx = blindIndexService.compute(
                        BlindIndexService.Kind.CLIENT_ID, blindIndexService.normalizeMrn(plainMrn));
            }
            if (fullNameCipher != null && !fullNameCipher.isBlank()) {
                String plainName = encryptionService.isEncrypted(fullNameCipher)
                        ? encryptionService.decrypt(fullNameCipher)
                        : fullNameCipher;
                if (plainName != null && !plainName.isBlank()) {
                    nameIdx = blindIndexService.compute(
                            BlindIndexService.Kind.FULL_NAME, blindIndexService.normalizeFullName(plainName));
                }
            }
            if (hasDobBlind) {
                String dobRaw = asString(row.get("date_of_birth"));
                if (dobRaw != null && !dobRaw.isBlank()) {
                    String plainDob = encryptionService.isEncrypted(dobRaw)
                            ? encryptionService.decrypt(dobRaw)
                            : dobRaw;
                    try {
                        dobIdx = blindIndexService.compute(
                                BlindIndexService.Kind.DATE_OF_BIRTH,
                                blindIndexService.normalizeDateOfBirth(java.time.LocalDate.parse(plainDob.trim())));
                    } catch (Exception ignored) {
                        // skip invalid historical DOB values
                    }
                }
            }

            // PostgreSQL aborts the whole transaction on unique violations. Catching the
            // Spring exception is not enough — use a savepoint so one duplicate MRN cannot
            // poison the batch (and leave name-token jobs starved / never advancing).
            DigestWriteResult write = writeClientDigestsWithSavepoint(
                    job.getSchemaName(), id, mrnIdx, nameIdx, dobIdx, hasDobBlind);
            if (write == DigestWriteResult.WRITTEN) {
                processed++;
            } else if (write == DigestWriteResult.DUPLICATE_MRN) {
                log.warn("PHI blind-index duplicate detected for client id={} in schema={} - skipping MRN digest (likely duplicate MRN)",
                        id, job.getSchemaName());
                // Name/DOB digests were still written on the retry path.
                processed++;
                skipped++;
            } else {
                skipped++;
            }
        }
        job.setLastProcessedId(maxId);
        if (skipped > 0) {
            log.info("PHI blind-index backfill batch completed: schema={} processed={} skipped={} (duplicates/errors)",
                    job.getSchemaName(), processed, skipped);
        }
        return processed;
    }

    private enum DigestWriteResult { WRITTEN, DUPLICATE_MRN, FAILED }

    /**
     * Apply client scalar digests inside a JDBC savepoint. On duplicate MRN digest, roll back
     * only the savepoint and retry name/DOB digests without the MRN digest.
     */
    private DigestWriteResult writeClientDigestsWithSavepoint(
            String schema, long id, byte[] mrnIdx, byte[] nameIdx, byte[] dobIdx, boolean hasDobBlind) {
        try {
            DigestWriteResult result = jdbcTemplate.execute((ConnectionCallback<DigestWriteResult>) connection -> {
                Savepoint savepoint = connection.setSavepoint("phi_client_digest_" + id);
                try {
                    executeClientDigestUpdate(connection, schema, id, mrnIdx, nameIdx, dobIdx, hasDobBlind);
                    connection.releaseSavepoint(savepoint);
                    return DigestWriteResult.WRITTEN;
                } catch (SQLException ex) {
                    connection.rollback(savepoint);
                    if (!isDuplicateClientIdBlindIdx(ex) || mrnIdx == null) {
                        log.warn("PHI blind-index update failed for client id={} in schema={}: {}",
                                id, schema, ex.getMessage());
                        return DigestWriteResult.FAILED;
                    }
                    // Keep name/DOB searchable even when MRN digest collides with another row.
                    Savepoint retry = connection.setSavepoint("phi_client_digest_retry_" + id);
                    try {
                        executeClientDigestUpdate(connection, schema, id, null, nameIdx, dobIdx, hasDobBlind);
                        connection.releaseSavepoint(retry);
                        return DigestWriteResult.DUPLICATE_MRN;
                    } catch (SQLException retryEx) {
                        connection.rollback(retry);
                        log.warn("PHI blind-index name/DOB retry failed for client id={} in schema={}: {}",
                                id, schema, retryEx.getMessage());
                        return DigestWriteResult.FAILED;
                    }
                }
            });
            return result != null ? result : DigestWriteResult.FAILED;
        } catch (DataIntegrityViolationException ex) {
            // Should be unreachable when savepoints work; keep batch alive if it surfaces.
            if (ex.getMessage() != null && ex.getMessage().contains("uq_clients_client_id_blind_idx")) {
                return DigestWriteResult.DUPLICATE_MRN;
            }
            log.warn("PHI blind-index constraint violation for client id={} in schema={}: {}",
                    id, schema, ex.getMessage());
            return DigestWriteResult.FAILED;
        } catch (Exception ex) {
            log.warn("PHI blind-index update failed for client id={} in schema={}: {}",
                    id, schema, ex.getMessage());
            return DigestWriteResult.FAILED;
        }
    }

    private static void executeClientDigestUpdate(
            java.sql.Connection connection,
            String schema,
            long id,
            byte[] mrnIdx,
            byte[] nameIdx,
            byte[] dobIdx,
            boolean hasDobBlind) throws SQLException {
        String sql = hasDobBlind
                ? "UPDATE " + schema
                + ".clients SET client_id_blind_idx = COALESCE(?, client_id_blind_idx), "
                + "full_name_blind_idx = COALESCE(?, full_name_blind_idx), "
                + "date_of_birth_blind_idx = COALESCE(?, date_of_birth_blind_idx) WHERE id = ?"
                : "UPDATE " + schema
                + ".clients SET client_id_blind_idx = COALESCE(?, client_id_blind_idx), "
                + "full_name_blind_idx = COALESCE(?, full_name_blind_idx) WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setBytes(1, mrnIdx);
            ps.setBytes(2, nameIdx);
            if (hasDobBlind) {
                ps.setBytes(3, dobIdx);
                ps.setLong(4, id);
            } else {
                ps.setLong(3, id);
            }
            ps.executeUpdate();
        }
    }

    private static boolean isDuplicateClientIdBlindIdx(SQLException ex) {
        String message = ex.getMessage();
        if (message != null && message.contains("uq_clients_client_id_blind_idx")) {
            return true;
        }
        for (Throwable cause = ex.getCause(); cause != null; cause = cause.getCause()) {
            if (cause.getMessage() != null && cause.getMessage().contains("uq_clients_client_id_blind_idx")) {
                return true;
            }
        }
        return "23505".equals(ex.getSQLState());
    }

    private static void resetJobForRerun(
            PlatformPhiBlindIndexBackfillJob job, String hmacKeyVersion, Instant now) {
        job.setStatus("PENDING");
        job.setLastProcessedId(0L);
        job.setProcessedCount(0L);
        job.setErrorCount(0L);
        job.setLastError(null);
        job.setHmacKeyVersion(hmacKeyVersion);
        job.setUpdatedAt(now);
    }

    /** Active clients with a name but zero rows in client_name_blind_indexes. */
    private long countClientsMissingNameTokens(String schema) {
        if (!hasTable(schema, "client_name_blind_indexes") || !hasTable(schema, "clients")) {
            return 0L;
        }
        String sql = """
                SELECT COUNT(*) FROM %s.clients c
                WHERE COALESCE(c.is_deleted, false) = false
                  AND c.full_name IS NOT NULL
                  AND btrim(c.full_name) <> ''
                  AND NOT EXISTS (
                    SELECT 1 FROM %s.client_name_blind_indexes t WHERE t.client_id = c.id
                  )
                """.formatted(schema, schema);
        Long count = jdbcTemplate.queryForObject(sql, Long.class);
        return count != null ? count : 0L;
    }

    private long maxClientId(String schema) {
        if (!hasTable(schema, "clients")) {
            return 0L;
        }
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM " + schema + ".clients", Long.class);
        return maxId != null ? maxId : 0L;
    }

    private long maxContactId(String schema) {
        if (!hasTable(schema, "client_contacts")) {
            return 0L;
        }
        Long maxId = jdbcTemplate.queryForObject(
                "SELECT COALESCE(MAX(id), 0) FROM " + schema + ".client_contacts", Long.class);
        return maxId != null ? maxId : 0L;
    }

    private int backfillContactDigests(PlatformPhiBlindIndexBackfillJob job, int limit) {
        // Walk every contact with a value (not only NULL digests). Stale non-null digests
        // from a wrong HMAC/pepper must be overwritten; NULL-only selection left them broken.
        String sql = """
                SELECT id, contact_type, contact_value
                FROM %s.client_contacts
                WHERE id > ?
                  AND contact_value IS NOT NULL
                  AND contact_value <> ''
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String typeRaw = asString(row.get("contact_type"));
            String cipher = asString(row.get("contact_value"));
            if (cipher == null || cipher.isBlank()) {
                continue;
            }
            String plain = encryptionService.decrypt(cipher);
            ContactType type = parseContactType(typeRaw);
            String normalized = blindIndexService.normalizeContactValue(type, plain);
            if (normalized == null || normalized.isBlank()) {
                continue;
            }
            byte[] digest = blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            jdbcTemplate.update(
                    "UPDATE " + job.getSchemaName() + ".client_contacts SET contact_blind_idx = ? WHERE id = ?",
                    digest, id);
            processed++;
        }
        job.setLastProcessedId(maxId);
        return processed;
    }

    /**
     * Backfill whitespace-separated name-token digests for first/last-name Client Page search.
     * Rebuilds token rows per client (delete + insert) so re-runs stay idempotent.
     */
    private int backfillNameTokenDigests(PlatformPhiBlindIndexBackfillJob job, int limit) {
        if (!hasTable(job.getSchemaName(), "client_name_blind_indexes")) {
            return 0;
        }
        String sql = """
                SELECT id, full_name
                FROM %s.clients
                WHERE id > ?
                  AND full_name IS NOT NULL
                  AND full_name <> ''
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String fullNameCipher = asString(row.get("full_name"));
            if (fullNameCipher == null || fullNameCipher.isBlank()) {
                continue;
            }
            String plainName = encryptionService.isEncrypted(fullNameCipher)
                    ? encryptionService.decrypt(fullNameCipher)
                    : fullNameCipher;
            if (plainName == null || plainName.isBlank()) {
                continue;
            }
            List<String> parts = blindIndexService.nameSearchTerms(plainName);
            jdbcTemplate.update(
                    "DELETE FROM " + job.getSchemaName() + ".client_name_blind_indexes WHERE client_id = ?",
                    id);
            int ord = 0;
            for (String part : parts) {
                if (part == null || part.isBlank()) {
                    continue;
                }
                byte[] digest = blindIndexService.compute(BlindIndexService.Kind.NAME_TOKEN, part);
                jdbcTemplate.update(
                        "INSERT INTO " + job.getSchemaName()
                                + ".client_name_blind_indexes (client_id, token_blind_idx, token_ord) VALUES (?, ?, ?)",
                        id, digest, ord++);
            }
            processed++;
        }
        job.setLastProcessedId(maxId);
        return processed;
    }

    private int encryptMrn(PlatformPhiBlindIndexBackfillJob job, int limit) {
        String sql = """
                SELECT id, client_id
                FROM %s.clients
                WHERE id > ?
                  AND client_id IS NOT NULL
                  AND client_id <> ''
                  AND client_id NOT LIKE 'TFENC:%%'
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String mrn = asString(row.get("client_id"));
            if (mrn == null || mrn.isBlank()) {
                continue;
            }
            String encrypted = encryptionService.encrypt(mrn);
            byte[] digest = blindIndexService.compute(
                    BlindIndexService.Kind.CLIENT_ID, blindIndexService.normalizeMrn(mrn));
            jdbcTemplate.update(
                    "UPDATE " + job.getSchemaName()
                            + ".clients SET client_id = ?, client_id_blind_idx = ? WHERE id = ?",
                    encrypted, digest, id);
            processed++;
        }
        job.setLastProcessedId(maxId);
        return processed;
    }

    private int reencryptNameToV2(PlatformPhiBlindIndexBackfillJob job, int limit) {
        return reencryptColumnToV2(job, limit, "clients", "full_name", BlindIndexService.Kind.FULL_NAME, true);
    }

    private int reencryptContactToV2(PlatformPhiBlindIndexBackfillJob job, int limit) {
        // Encrypt legacy plaintext and migrate deterministic TFENC:v2d → random TFENC:v2.
        // (Earlier cutover only matched v2d, so pre-converter plaintext contacts were skipped.)
        String sql = """
                SELECT id, contact_type, contact_value
                FROM %s.client_contacts
                WHERE id > ?
                  AND contact_value IS NOT NULL
                  AND btrim(contact_value) <> ''
                  AND (
                    contact_value LIKE 'TFENC:v2d:%%'
                    OR contact_value NOT LIKE 'TFENC:%%'
                  )
                ORDER BY id ASC
                LIMIT ?
                """.formatted(job.getSchemaName());
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String stored = asString(row.get("contact_value"));
            if (stored == null || stored.isBlank()) {
                continue;
            }
            String plain = encryptionService.isEncrypted(stored)
                    ? encryptionService.decrypt(stored)
                    : stored;
            ContactType type = parseContactType(asString(row.get("contact_type")));
            String normalized = blindIndexService.normalizeContactValue(type, plain);
            String encrypted = encryptionService.encrypt(plain);
            byte[] digest = (normalized == null || normalized.isBlank())
                    ? null
                    : blindIndexService.compute(BlindIndexService.Kind.CONTACT, normalized);
            jdbcTemplate.update(
                    "UPDATE " + job.getSchemaName()
                            + ".client_contacts SET contact_value = ?, contact_blind_idx = COALESCE(?, contact_blind_idx) WHERE id = ?",
                    encrypted, digest, id);
            processed++;
        }
        job.setLastProcessedId(maxId);
        return processed;
    }

    private int reencryptColumnToV2(
            PlatformPhiBlindIndexBackfillJob job,
            int limit,
            String table,
            String column,
            BlindIndexService.Kind kind,
            boolean updateNameBlind) {
        String sql = """
                SELECT id, %s
                FROM %s.%s
                WHERE id > ?
                  AND %s IS NOT NULL
                  AND btrim(%s) <> ''
                  AND (
                    %s LIKE 'TFENC:v2d:%%'
                    OR %s NOT LIKE 'TFENC:%%'
                  )
                ORDER BY id ASC
                LIMIT ?
                """.formatted(column, job.getSchemaName(), table, column, column, column, column);
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, job.getLastProcessedId(), limit);
        if (rows.isEmpty()) {
            return 0;
        }
        long maxId = job.getLastProcessedId();
        int processed = 0;
        for (Map<String, Object> row : rows) {
            long id = ((Number) row.get("id")).longValue();
            maxId = Math.max(maxId, id);
            String stored = asString(row.get(column));
            if (stored == null || stored.isBlank()) {
                continue;
            }
            String plain = encryptionService.isEncrypted(stored)
                    ? encryptionService.decrypt(stored)
                    : stored;
            String encrypted = encryptionService.encrypt(plain);
            if (updateNameBlind && plain != null && !plain.isBlank()) {
                byte[] digest = blindIndexService.compute(kind, blindIndexService.normalizeFullName(plain));
                jdbcTemplate.update(
                        "UPDATE " + job.getSchemaName() + "." + table
                                + " SET " + column + " = ?, full_name_blind_idx = ? WHERE id = ?",
                        encrypted, digest, id);
            } else {
                jdbcTemplate.update(
                        "UPDATE " + job.getSchemaName() + "." + table
                                + " SET " + column + " = ? WHERE id = ?",
                        encrypted, id);
            }
            processed++;
        }
        job.setLastProcessedId(maxId);
        return processed;
    }

    private boolean hasBlindIndexColumn(String schema) {
        return hasColumn(schema, "clients", "client_id_blind_idx");
    }

    private boolean hasTable(String schema, String table) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.tables
                    WHERE table_schema = ? AND table_name = ?
                    """,
                    Integer.class, schema, table);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean hasColumn(String schema, String table, String column) {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    """
                    SELECT COUNT(*) FROM information_schema.columns
                    WHERE table_schema = ? AND table_name = ? AND column_name = ?
                    """,
                    Integer.class, schema, table, column);
            return count != null && count > 0;
        } catch (Exception e) {
            return false;
        }
    }

    private Set<String> parseAllowList() {
        if (schemaAllowList == null || schemaAllowList.isBlank()) {
            return Set.of();
        }
        return Stream.of(schemaAllowList.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> s.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private static ContactType parseContactType(String raw) {
        if (raw == null || raw.isBlank()) {
            return ContactType.EMAIL;
        }
        try {
            return ContactType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            return ContactType.EMAIL;
        }
    }

    private static String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private static void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + value);
        }
    }

    /**
     * Computes a version hash of the current HMAC key material.
     * When this changes, completed jobs are automatically reset for re-indexing.
     *
     * @return First 16 characters of SHA-256 hash of HMAC key material, or null if unavailable
     */
    private String computeHmacKeyVersion() {
        try {
            byte[] keyMaterial = blindIndexService.getKeyProvider().getSearchHmacKey();
            if (keyMaterial == null || keyMaterial.length == 0) {
                return null;
            }
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(keyMaterial);
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < Math.min(8, hash.length); i++) {
                String hex = Integer.toHexString(0xff & hash[i]);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            log.error("Failed to compute HMAC key version: {}", e.getMessage());
            return null;
        }
    }
}
