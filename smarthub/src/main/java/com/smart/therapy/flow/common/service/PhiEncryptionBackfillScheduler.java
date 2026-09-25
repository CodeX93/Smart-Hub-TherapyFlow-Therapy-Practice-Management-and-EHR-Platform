package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.common.entity.PlatformPhiEncryptionBackfillJob;
import com.smart.therapy.flow.common.repository.PlatformPhiEncryptionBackfillJobRepository;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Restartable platform job that migrates tenant PHI columns to {@code TFENC:v2}:
 * plaintext, Jasypt {@code ENC(...)}, and {@code TFENC:v2d} → randomized {@code TFENC:v2}.
 * Enable with {@code app.phi-encryption.backfill.enabled=true} during cutover.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PhiEncryptionBackfillScheduler {

    private static final Pattern IDENTIFIER = Pattern.compile("[a-zA-Z0-9_]+");
    private static final String ENCRYPTED_PREFIX = "TFENC:%";

    /** Wave A — clinical narrative and report text (randomized encryption). */
    private static final List<ColumnTarget> WAVE_A_COLUMNS = List.of(
            new ColumnTarget("session_notes", "session_focus", false),
            new ColumnTarget("session_notes", "symptoms", false),
            new ColumnTarget("session_notes", "short_term_goals", false),
            new ColumnTarget("session_notes", "intervention", false),
            new ColumnTarget("session_notes", "progress", false),
            new ColumnTarget("session_notes", "remarks", false),
            new ColumnTarget("session_notes", "recommendations", false),
            new ColumnTarget("session_notes", "generated_content", false),
            new ColumnTarget("session_notes", "draft_content", false),
            new ColumnTarget("session_notes", "final_content", false),
            new ColumnTarget("session_notes", "custom_ai_prompt", false),
            new ColumnTarget("session_transcripts", "final_transcript", false),
            new ColumnTarget("session_transcripts", "raw_content", false),
            new ColumnTarget("session_transcript_chunks", "chunk_text", false),
            new ColumnTarget("client_reports", "generated_content", false),
            new ColumnTarget("client_reports", "draft_content", false),
            new ColumnTarget("client_reports", "final_content", false),
            new ColumnTarget("assessment_reports", "generated_content", false),
            new ColumnTarget("assessment_reports", "draft_content", false),
            new ColumnTarget("assessment_reports", "final_content", false),
            new ColumnTarget("assessment_reports", "report_data", false)
    );

    /** Wave B — client identifiers and related PHI (random TFENC:v2; search via blind indexes). */
    private static final List<ColumnTarget> WAVE_B_COLUMNS = List.of(
            new ColumnTarget("clients", "full_name", false),
            new ColumnTarget("clients", "date_of_birth", false),
            new ColumnTarget("clients", "gender", false),
            new ColumnTarget("clients", "marital_status", false),
            new ColumnTarget("clients", "preferred_language", false),
            new ColumnTarget("clients", "pronouns", false),
            new ColumnTarget("clients", "client_type", false),
            new ColumnTarget("clients", "service_type", false),
            new ColumnTarget("clients", "service_frequency", false),
            new ColumnTarget("clients", "treatment_modality", false),
            new ColumnTarget("client_contacts", "contact_value", false),
            new ColumnTarget("client_contacts", "contact_person_name", false),
            new ColumnTarget("client_contacts", "relationship", false),
            new ColumnTarget("client_contacts", "label", false),
            new ColumnTarget("client_contacts", "notes", false),
            new ColumnTarget("client_addresses", "street_address_1", false),
            new ColumnTarget("client_addresses", "street_address_2", false),
            new ColumnTarget("client_addresses", "city", false),
            new ColumnTarget("client_addresses", "state_province", false),
            new ColumnTarget("client_addresses", "postal_code", false),
            new ColumnTarget("client_addresses", "country", false),
            new ColumnTarget("client_addresses", "address_legacy", false),
            new ColumnTarget("client_addresses", "state_legacy", false),
            new ColumnTarget("client_addresses", "zip_code_legacy", false),
            new ColumnTarget("client_addresses", "notes", false),
            new ColumnTarget("client_insurance", "insurance_provider", false),
            new ColumnTarget("client_insurance", "insurance_type", false),
            new ColumnTarget("client_insurance", "policy_number", false),
            new ColumnTarget("client_insurance", "group_number", false),
            new ColumnTarget("client_insurance", "subscriber_name", false),
            new ColumnTarget("client_insurance", "subscriber_relationship", false),
            new ColumnTarget("client_insurance", "insurance_phone", false),
            new ColumnTarget("client_insurance", "insurance_email", false),
            new ColumnTarget("client_insurance", "authorization_number", false),
            new ColumnTarget("client_insurance", "verified_by", false),
            new ColumnTarget("client_insurance", "notes", false),
            new ColumnTarget("client_history", "from_value", false),
            new ColumnTarget("client_history", "to_value", false),
            new ColumnTarget("client_history", "metadata", false),
            new ColumnTarget("client_history", "description", false),
            new ColumnTarget("client_history", "created_by_name", false),
            new ColumnTarget("report_supporting_files", "original_name", false),
            new ColumnTarget("report_supporting_files", "extracted_text", false)
    );

    /** Wave 1 — remaining clinical/demographic PHI (exclude transcripts/recording). */
    private static final List<ColumnTarget> WAVE_1_COLUMNS = List.of(
            new ColumnTarget("sessions", "notes", false),
            new ColumnTarget("sessions", "billing_notes", false),
            new ColumnTarget("session_note_amendments", "amendment_text", false),
            new ColumnTarget("session_note_amendments", "reason", false),
            new ColumnTarget("assessment_responses", "response_text", false),
            new ColumnTarget("assessment_responses", "response_value", false),
            new ColumnTarget("assessment_assignments", "notes", false),
            new ColumnTarget("patient_consents", "signed_by", false),
            new ColumnTarget("patient_consents", "witness_name", false),
            new ColumnTarget("patient_consents", "witness_signature", false),
            new ColumnTarget("patient_consents", "legal_guardian_name", false),
            new ColumnTarget("patient_consents", "legal_guardian_relationship", false),
            new ColumnTarget("patient_consents", "legal_guardian_signature", false),
            // patient_consents.signature_data is excluded: it is a Postgres large-object
            // (oid) column, not text, and PatientConsent.signatureData already encrypts
            // via EncryptedStringConverter inside the large object on every write.
            new ColumnTarget("patient_consents", "consent_document", false),
            new ColumnTarget("patient_consents", "consent_form_url", false),
            new ColumnTarget("patient_consents", "notes", false),
            new ColumnTarget("clients", "notes", false),
            new ColumnTarget("clients", "follow_up_notes", false),
            new ColumnTarget("client_referrals", "referral_source", false),
            new ColumnTarget("client_referrals", "referrer_name", false),
            new ColumnTarget("client_referrals", "referrer_title", false),
            new ColumnTarget("client_referrals", "referrer_organization", false),
            new ColumnTarget("client_referrals", "referrer_phone", false),
            new ColumnTarget("client_referrals", "referrer_email", false),
            new ColumnTarget("client_referrals", "reference_number", false),
            new ColumnTarget("client_referrals", "client_source", false),
            new ColumnTarget("client_referrals", "court_order_number", false),
            new ColumnTarget("client_referrals", "court_jurisdiction", false),
            new ColumnTarget("client_referrals", "reporting_recipient", false),
            new ColumnTarget("client_referrals", "reporting_frequency", false),
            new ColumnTarget("client_referrals", "marketing_campaign", false),
            new ColumnTarget("client_referrals", "promo_code", false),
            new ColumnTarget("client_referrals", "referral_notes", false),
            new ColumnTarget("client_referrals", "intake_summary", false),
            new ColumnTarget("client_employment", "employment_status", false),
            new ColumnTarget("client_employment", "employer_name", false),
            new ColumnTarget("client_employment", "job_title", false),
            new ColumnTarget("client_employment", "education_level", false),
            new ColumnTarget("client_employment", "field_of_study", false),
            new ColumnTarget("client_employment", "school_name", false),
            new ColumnTarget("client_employment", "occupation_category", false),
            new ColumnTarget("client_employment", "disability_status", false),
            new ColumnTarget("client_employment", "military_branch", false),
            new ColumnTarget("client_employment", "notes", false),
            new ColumnTarget("form_signatures", "signer_name", false),
            new ColumnTarget("form_signatures", "signer_email", false),
            new ColumnTarget("documents", "original_name", false),
            new ColumnTarget("documents", "description", false)
    );

    private static final List<ColumnTarget> BACKFILL_COLUMNS = Stream.of(
            WAVE_A_COLUMNS.stream(),
            WAVE_B_COLUMNS.stream(),
            WAVE_1_COLUMNS.stream()
    ).flatMap(s -> s).toList();

    private final PlatformPhiEncryptionBackfillJobRepository jobRepository;
    private final OrganisationRepository organisationRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final EncryptionService encryptionService;
    private final JdbcTemplate jdbcTemplate;

    @Value("${app.phi-encryption.backfill.enabled:false}")
    private boolean enabled;

    @Value("${app.phi-encryption.backfill.batch-size:50}")
    private int batchSize;

    /** Comma-separated schema names; empty = all tenant schemas. */
    @Value("${app.phi-encryption.backfill.schema-allow-list:}")
    private String schemaAllowList;

    @Scheduled(fixedDelayString = "${app.phi-encryption.backfill.fixed-delay-ms:120000}")
    public void runBackfillTick() {
        if (!enabled) {
            return;
        }
        seedMissingJobs();
        cancelNonAllowListedPendingJobs();
        cancelExcludedColumnJobs();
        processOneJob();
    }

    /** Cancel leftover jobs for columns excluded from encryption (e.g. voice_transcription). */
    @Transactional
    protected void cancelExcludedColumnJobs() {
        Instant now = Instant.now();
        for (PlatformPhiEncryptionBackfillJob job : jobRepository.findRunnableJobs()) {
            if (!"voice_transcription".equals(job.getColumnName())) {
                continue;
            }
            cancelJob(job, "cancelled: voice_transcription excluded from PHI encryption backfill", now);
        }
        // The oid column made these jobs fail before removal from the target list, and
        // FAILED jobs are not runnable, so they must be swept by table/column instead.
        for (PlatformPhiEncryptionBackfillJob job
                : jobRepository.findByTableNameAndColumnName("patient_consents", "signature_data")) {
            if ("CANCELLED".equals(job.getStatus()) || "COMPLETED".equals(job.getStatus())) {
                continue;
            }
            cancelJob(job, "cancelled: patient_consents.signature_data is an oid large object;"
                    + " values are encrypted by EncryptedStringConverter on write", now);
        }
    }

    private void cancelJob(PlatformPhiEncryptionBackfillJob job, String reason, Instant now) {
        job.setStatus("CANCELLED");
        job.setLastError(reason);
        job.setUpdatedAt(now);
        jobRepository.save(job);
    }

    @Transactional
    protected void seedMissingJobs() {
        Instant now = Instant.now();
        Set<String> allow = parseAllowList();
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
            for (ColumnTarget target : BACKFILL_COLUMNS) {
                if (jobRepository.findBySchemaNameAndTableNameAndColumnName(
                        schema, target.tableName(), target.columnName()).isPresent()) {
                    continue;
                }
                jobRepository.save(PlatformPhiEncryptionBackfillJob.builder()
                        .schemaName(schema)
                        .tableName(target.tableName())
                        .columnName(target.columnName())
                        .pkColumn("id")
                        .status("PENDING")
                        .lastProcessedId(0L)
                        .processedCount(0L)
                        .errorCount(0L)
                        .createdAt(now)
                        .updatedAt(now)
                        .build());
            }
        }
    }

    @Transactional
    protected void cancelNonAllowListedPendingJobs() {
        Set<String> allow = parseAllowList();
        if (allow.isEmpty()) {
            return;
        }
        List<PlatformPhiEncryptionBackfillJob> runnable = jobRepository.findRunnableJobs();
        Instant now = Instant.now();
        for (PlatformPhiEncryptionBackfillJob job : runnable) {
            String schema = job.getSchemaName() == null ? "" : job.getSchemaName().toLowerCase(Locale.ROOT);
            if (allow.contains(schema)) {
                continue;
            }
            job.setStatus("CANCELLED");
            job.setLastError("cancelled: schema not in app.phi-encryption.backfill.schema-allow-list");
            job.setUpdatedAt(now);
            jobRepository.save(job);
        }
    }

    @Transactional
    protected void processOneJob() {
        Set<String> allow = parseAllowList();
        List<PlatformPhiEncryptionBackfillJob> runnable = jobRepository.findRunnableJobs();
        PlatformPhiEncryptionBackfillJob job = runnable.stream()
                .filter(j -> {
                    if (allow.isEmpty()) {
                        return true;
                    }
                    String schema = j.getSchemaName() == null ? "" : j.getSchemaName().toLowerCase(Locale.ROOT);
                    return allow.contains(schema);
                })
                .findFirst()
                .orElse(null);
        if (job == null) {
            return;
        }
        job.setStatus("IN_PROGRESS");
        job.setUpdatedAt(Instant.now());
        jobRepository.save(job);

        int safeBatch = Math.max(1, Math.min(batchSize, 50));
        try {
            validateIdentifier(job.getSchemaName());
            validateIdentifier(job.getTableName());
            validateIdentifier(job.getColumnName());
            validateIdentifier(job.getPkColumn());

            ColumnTarget target = BACKFILL_COLUMNS.stream()
                    .filter(t -> t.tableName().equals(job.getTableName())
                            && t.columnName().equals(job.getColumnName()))
                    .findFirst()
                    .orElse(new ColumnTarget(job.getTableName(), job.getColumnName(), false));

            String qualifiedTable = job.getSchemaName() + "." + job.getTableName();
            String skipPrefix = target.deterministic() ? "TFENC:v2d:%" : "TFENC:v2:%";
            String selectSql = """
                    SELECT %s, %s
                    FROM %s
                    WHERE %s > ?
                      AND %s IS NOT NULL
                      AND %s <> ''
                      AND %s NOT LIKE ?
                    ORDER BY %s ASC
                    LIMIT ?
                    """.formatted(
                    job.getPkColumn(),
                    job.getColumnName(),
                    qualifiedTable,
                    job.getPkColumn(),
                    job.getColumnName(),
                    job.getColumnName(),
                    job.getColumnName(),
                    job.getPkColumn());

            List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                    selectSql, job.getLastProcessedId(), skipPrefix, safeBatch);

            if (rows.isEmpty()) {
                job.setStatus("COMPLETED");
                job.setUpdatedAt(Instant.now());
                jobRepository.save(job);
                log.info("PHI backfill completed for {}.{}.{}",
                        job.getSchemaName(), job.getTableName(), job.getColumnName());
                return;
            }

            String updateSql = "UPDATE " + qualifiedTable + " SET " + job.getColumnName()
                    + " = ? WHERE " + job.getPkColumn() + " = ?";
            long maxId = job.getLastProcessedId();
            long processed = 0;

            for (Map<String, Object> row : rows) {
                Object idValue = row.get(job.getPkColumn());
                Object plaintextValue = row.get(job.getColumnName());
                if (idValue == null || plaintextValue == null) {
                    continue;
                }
                long rowId = ((Number) idValue).longValue();
                String purpose = job.getTableName() + "." + job.getColumnName();
                String value = plaintextValue.toString();
                if (value.isBlank()) {
                    maxId = Math.max(maxId, rowId);
                    continue;
                }

                String plaintext = value;
                if (encryptionService.isEncrypted(value)) {
                    boolean alreadyTargetFormat = target.deterministic()
                            ? value.startsWith("TFENC:v2d:")
                            : value.startsWith("TFENC:v2:");
                    if (alreadyTargetFormat) {
                        maxId = Math.max(maxId, rowId);
                        continue;
                    }
                    // Migrate Jasypt ENC(...), TFENC:v2d → v2 (or the reverse for deterministic).
                    plaintext = encryptionService.decrypt(value);
                }
                String encrypted = target.deterministic()
                        ? encryptionService.encryptDeterministic(plaintext, purpose)
                        : encryptionService.encrypt(plaintext);
                jdbcTemplate.update(updateSql, encrypted, rowId);
                processed++;
                maxId = Math.max(maxId, rowId);
            }

            job.setLastProcessedId(maxId);
            job.setProcessedCount(job.getProcessedCount() + processed);
            job.setStatus("PENDING");
            job.setLastError(null);
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            log.debug("PHI backfill batch processed {} rows for {}.{}.{}",
                    processed, job.getSchemaName(), job.getTableName(), job.getColumnName());
        } catch (Exception ex) {
            job.setStatus("FAILED");
            job.setErrorCount(job.getErrorCount() + 1);
            job.setLastError(ex.getMessage());
            job.setUpdatedAt(Instant.now());
            jobRepository.save(job);
            log.error("PHI backfill failed for {}.{}.{}: {}",
                    job.getSchemaName(), job.getTableName(), job.getColumnName(), ex.getMessage());
        }
    }

    private static void validateIdentifier(String value) {
        if (value == null || !IDENTIFIER.matcher(value).matches()) {
            throw new IllegalArgumentException("Invalid SQL identifier: " + value);
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

    private record ColumnTarget(String tableName, String columnName, boolean deterministic) {
    }
}
