package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.AuthProvider;
import com.smart.therapy.flow.auth.entity.IdentityType;
import com.smart.therapy.flow.auth.repository.AuthIdentityRepository;
import com.smart.therapy.flow.auth.repository.UserRepository;
import com.smart.therapy.flow.auth.service.AuthIdentityService;
import com.smart.therapy.flow.auth.service.AuthSessionService;
import com.smart.therapy.flow.common.service.EmailService;
import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.repository.UserOrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.subscription.enums.InvoiceStatus;
import com.smart.therapy.flow.subscription.repository.InvoiceRepository;
import com.smart.therapy.flow.superadmin.entity.PlatformPurgeJob;
import com.smart.therapy.flow.superadmin.entity.PlatformBackupJob;
import com.smart.therapy.flow.superadmin.entity.PlatformPasswordResetJob;
import com.smart.therapy.flow.superadmin.repository.PlatformBackupJobRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformPasswordResetJobRepository;
import com.smart.therapy.flow.superadmin.repository.PlatformPurgeJobRepository;
import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.common.security.TokenBlacklistService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminOperationsService {

    private static final String BACKUP_UNSUPPORTED_MESSAGE =
            "Tenant backups are unsupported until Azure backup integration is implemented";

    private final OrganisationRepository organisationRepository;
    private final PlatformBackupJobRepository backupJobRepository;
    private final TenantDirectoryService tenantDirectoryService;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final PlatformAuditService platformAuditService;
    private final UserOrganisationRepository userOrganisationRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final InvoiceRepository invoiceRepository;
    private final PlatformPurgeJobRepository platformPurgeJobRepository;
    private final PlatformPasswordResetJobRepository platformPasswordResetJobRepository;
    private final Optional<RedisTemplate<String, Object>> redisTemplate;
    private final SuperAdminNotificationService superAdminNotificationService;
    private final AuthIdentityRepository authIdentityRepository;
    private final AuthIdentityService authIdentityService;
    private final AuthSessionService authSessionService;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    @Value("${superadmin.jobs.backups.enabled:true}")
    private boolean backupsEnabled;
    @Value("${superadmin.jobs.password-reset.enabled:true}")
    private boolean passwordResetEnabled;
    @Value("${superadmin.jobs.purge.enabled:true}")
    private boolean purgeEnabled;
    @Value("${superadmin.jobs.purge-warning.enabled:true}")
    private boolean purgeWarningEnabled;
    @Value("${app.redis.fail-open:true}")
    private boolean redisFailOpen;
    private volatile Boolean backupsEnabledOverride;
    private volatile Boolean passwordResetEnabledOverride;
    private volatile Boolean purgeEnabledOverride;
    private volatile Boolean purgeWarningEnabledOverride;
    private static final Pattern SAFE_SCHEMA = Pattern.compile("[a-zA-Z0-9_]+");

    @Transactional
    public Organisation suspendOrganisation(Long orgId, Long actorAuthId, String reason) {
        Organisation org = requireOrganisation(orgId);
        if ("LOCKED".equalsIgnoreCase(org.getStatus())) {
            throw new StoryApiException(HttpStatus.CONFLICT, "ALREADY_SUSPENDED", "Organisation is already suspended");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() < 5 || reason.trim().length() > 500) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "reason is required (5-500 chars)");
        }
        String before = org.getStatus();
        org.setStatus("LOCKED");
        org.setForceDisabledReason(reason.trim());
        Organisation saved = organisationRepository.save(org);
        revokeOrgSessions(saved.getId());
        refreshTenantCache(saved);
        platformAuditService.log(actorAuthId, "ORGANISATION_SUSPENDED", "Organisation", String.valueOf(orgId),
                "reason=" + reason.trim() + ", beforeStatus=" + before + ", afterStatus=" + saved.getStatus());
        return saved;
    }

    @Transactional
    public Organisation reactivateOrganisation(Long orgId, Long actorAuthId, String reason) {
        Organisation org = requireOrganisation(orgId);
        if (!"LOCKED".equalsIgnoreCase(org.getStatus())) {
            throw new StoryApiException(HttpStatus.CONFLICT, "NOT_SUSPENDED", "Organisation is not suspended");
        }
        String before = org.getStatus();
        org.setStatus("ACTIVE");
        org.setForceDisabledReason(null);
        Organisation saved = organisationRepository.save(org);
        refreshTenantCache(saved);
        platformAuditService.log(actorAuthId, "ORGANISATION_REACTIVATED", "Organisation", String.valueOf(orgId),
                "reason=" + (reason == null ? "" : reason.trim()) + ", beforeStatus=" + before + ", afterStatus=" + saved.getStatus());
        return saved;
    }

    @Transactional
    public Organisation terminateOrganisation(Long orgId, Long actorAuthId, String reason, Integer retentionDays) {
        Organisation org = requireOrganisation(orgId);
        if ("ARCHIVED".equalsIgnoreCase(org.getStatus()) && org.getTerminationEffectiveAt() != null) {
            throw new StoryApiException(HttpStatus.CONFLICT, "ALREADY_SCHEDULED", "Termination is already scheduled");
        }
        if (retentionDays == null || retentionDays < 30 || retentionDays > 365) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "RETENTION_OUT_OF_RANGE", "retentionDays must be between 30 and 365");
        }
        if (reason == null || reason.isBlank() || reason.trim().length() < 5 || reason.trim().length() > 500) {
            throw new StoryApiException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "reason is required (5-500 chars)");
        }
        if (invoiceRepository.existsOpenInvoicesByOrganisationId(orgId,
                List.of(InvoiceStatus.PENDING, InvoiceStatus.PAST_DUE, InvoiceStatus.FAILED))) {
            throw new StoryApiException(HttpStatus.UNPROCESSABLE_ENTITY, "OPEN_INVOICES", "Organisation has open invoices");
        }
        Instant scheduledAt = Instant.now().plusSeconds(retentionDays.longValue() * 24 * 3600);
        org.setStatus("ARCHIVED");
        org.setTerminationEffectiveAt(scheduledAt);
        org.setForceDisabledReason(reason.trim());
        Organisation saved = organisationRepository.save(org);
        PlatformPurgeJob job = PlatformPurgeJob.builder()
                .organisation(saved)
                .retentionDays(retentionDays)
                .reason(reason.trim())
                .status("pending")
                .scheduledAt(scheduledAt)
                .createdByAuthId(actorAuthId)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        platformPurgeJobRepository.save(job);
        revokeOrgSessions(saved.getId());
        refreshTenantCache(saved);
        platformAuditService.log(actorAuthId, "ORGANISATION_TERMINATION_SCHEDULED", "Organisation", String.valueOf(orgId),
                "reason=" + reason.trim() + ", retentionDays=" + retentionDays + ", effectiveAt=" + scheduledAt);
        return saved;
    }

    @Transactional
    public PlatformBackupJob requestBackup(Long orgId, Long actorAuthId) {
        throw backupUnsupported();
    }

    @Transactional(readOnly = true)
    public List<PlatformBackupJob> listBackupJobs(Long orgId) {
        if (orgId == null) {
            return backupJobRepository.findTop100ByOrderByCreatedAtDesc();
        }
        return backupJobRepository.findByOrganisation_IdOrderByCreatedAtDesc(orgId);
    }

    @Scheduled(fixedDelayString = "${superadmin.backups.dispatch-ms:45000}")
    @Transactional
    public void processQueuedBackups() {
        if (!isBackupsEnabled()) {
            return;
        }
        processQueuedBackupsInternal();
    }

    @Transactional
    public void runBackupsNow() {
        processQueuedBackupsInternal();
    }

    public void setBackupsEnabledOverride(Boolean enabled) {
        this.backupsEnabledOverride = enabled;
    }

    public Boolean getBackupsEnabledOverride() {
        return backupsEnabledOverride;
    }

    public boolean isBackupsEnabled() {
        return backupsEnabledOverride != null ? backupsEnabledOverride : backupsEnabled;
    }

    private void processQueuedBackupsInternal() {
        List<PlatformBackupJob> queue = backupJobRepository.findByStatusInOrderByCreatedAtAsc(List.of("queued"));
        for (PlatformBackupJob job : queue) {
            Instant now = Instant.now();
            job.setStatus("failed");
            job.setStorageLocation(null);
            job.setCompletedAt(null);
            job.setErrorMessage(BACKUP_UNSUPPORTED_MESSAGE);
            job.setUpdatedAt(now);
            backupJobRepository.save(job);

            Organisation org = job.getOrganisation();
            org.setBackupStatus("FAILED");
            org.setBackupLocation(null);
            organisationRepository.save(org);
            refreshTenantCache(org);
        }
    }

    @Transactional
    public Map<String, Object> applyBulkAction(List<Long> organisationIds, String action, Long actorAuthId, String reason) {
        if (organisationIds == null || organisationIds.isEmpty()) {
            throw new IllegalArgumentException("organisationIds is required");
        }
        if ("backup".equalsIgnoreCase(action)) {
            throw backupUnsupported();
        }
        int success = 0;
        int failed = 0;
        for (Long orgId : organisationIds) {
            try {
                switch (action.toLowerCase()) {
                    case "suspend" -> suspendOrganisation(orgId, actorAuthId, reason);
                    case "reactivate" -> reactivateOrganisation(orgId, actorAuthId, reason);
                    case "terminate" -> terminateOrganisation(orgId, actorAuthId, reason, 30);
                    case "backup" -> requestBackup(orgId, actorAuthId);
                    default -> throw new IllegalArgumentException("Unsupported action: " + action);
                }
                success++;
            } catch (Exception ex) {
                failed++;
            }
        }
        return Map.of("action", action, "requested", organisationIds.size(), "success", success, "failed", failed);
    }

    private StoryApiException backupUnsupported() {
        return new StoryApiException(
                HttpStatus.NOT_IMPLEMENTED,
                "AZURE_BACKUP_UNSUPPORTED",
                BACKUP_UNSUPPORTED_MESSAGE
        );
    }

    @Transactional
    public PasswordResetActionResult forceOrganisationPasswordReset(Long orgId,
                                                                    Instant effectiveAt,
                                                                    Long actorAuthId,
                                                                    String reason) {
        Organisation org = requireOrganisation(orgId);
        Instant now = Instant.now();
        Instant scheduleAt = effectiveAt != null ? effectiveAt : now;
        if (scheduleAt.isAfter(now)) {
            PlatformPasswordResetJob job = PlatformPasswordResetJob.builder()
                    .organisation(org)
                    .status("pending")
                    .effectiveAt(scheduleAt)
                    .reason(reason)
                    .requestedByAuthId(actorAuthId)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();
            job = platformPasswordResetJobRepository.save(job);
            platformAuditService.log(actorAuthId, "ORG_FORCE_PASSWORD_RESET_SCHEDULED", "Organisation", String.valueOf(orgId),
                    "effectiveAt=" + scheduleAt + ", reason=" + (reason == null ? "" : reason));
            return new PasswordResetActionResult("scheduled", scheduleAt, job.getId(), 0, 0);
        }
        ExecutionSummary summary = executeForcePasswordReset(org, actorAuthId, reason);
        return new PasswordResetActionResult("executed", scheduleAt, null, summary.affectedUsers(), summary.skippedUsers());
    }

    private Organisation requireOrganisation(Long orgId) {
        return organisationRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("Organisation not found: " + orgId));
    }

    @Scheduled(fixedDelayString = "${superadmin.password-reset.dispatch-ms:45000}")
    @Transactional
    public void processScheduledPasswordResetJobs() {
        if (!isPasswordResetEnabled()) {
            return;
        }
        processScheduledPasswordResetJobsInternal();
    }

    @Transactional
    public void runPasswordResetDispatchNow() {
        processScheduledPasswordResetJobsInternal();
    }

    public void setPasswordResetEnabledOverride(Boolean enabled) {
        this.passwordResetEnabledOverride = enabled;
    }

    public Boolean getPasswordResetEnabledOverride() {
        return passwordResetEnabledOverride;
    }

    public boolean isPasswordResetEnabled() {
        return passwordResetEnabledOverride != null ? passwordResetEnabledOverride : passwordResetEnabled;
    }

    private void processScheduledPasswordResetJobsInternal() {
        Instant now = Instant.now();
        List<PlatformPasswordResetJob> due = platformPasswordResetJobRepository
                .findByStatusAndEffectiveAtLessThanEqualOrderByEffectiveAtAsc("pending", now);
        for (PlatformPasswordResetJob job : due) {
            String lockKey = "password-reset-job:" + job.getId();
            if (!acquireLock(lockKey)) {
                continue;
            }
            try {
                ExecutionSummary summary = executeForcePasswordReset(job.getOrganisation(), job.getRequestedByAuthId(), job.getReason());
                job.setStatus("completed");
                job.setProcessedAt(Instant.now());
                job.setUpdatedAt(Instant.now());
                job.setErrorMessage("affected=" + summary.affectedUsers() + ", skipped=" + summary.skippedUsers());
                platformPasswordResetJobRepository.save(job);
            } catch (Exception ex) {
                job.setStatus("failed");
                job.setErrorMessage(ex.getMessage());
                job.setUpdatedAt(Instant.now());
                platformPasswordResetJobRepository.save(job);
            } finally {
                releaseLock(lockKey);
            }
        }
    }

    private ExecutionSummary executeForcePasswordReset(Organisation org, Long actorAuthId, String reason) {
        Set<Long> authIds = new HashSet<>(userOrganisationRepository.findDistinctAuthIdsByOrganisationId(org.getId()));
        authIdentityRepository.findByOrganisation_Id(org.getId()).forEach(identity -> authIds.add(identity.getId()));

        int affected = 0;
        int skipped = 0;
        Instant expiry = Instant.now().plusSeconds(24 * 3600L);

        for (Long authId : authIds) {
            AuthIdentity identity = authIdentityRepository.findById(authId).orElse(null);
            if (identity == null || !Boolean.TRUE.equals(identity.getIsActive())) {
                skipped++;
                continue;
            }
            if (!AuthProvider.LOCAL.equals(identity.getAuthProvider())) {
                skipped++;
                continue;
            }
            if (IdentityType.CLIENT.equals(identity.getIdentityType())) {
                skipped++;
                continue;
            }

            String resetToken = authIdentityService.generateSecureToken();
            authIdentityService.setPasswordResetToken(authId, resetToken, expiry);
            authSessionService.revokeAllForAuthId(authId);
            tokenBlacklistService.blacklistAllUserTokens(authId);
            String email = identity.getLoginIdentifier();
            if (email != null && !email.isBlank()) {
                String fullName = tenantTransactionExecutor.executeReadOnly(
                        org.getId(),
                        org.getSchemaName(),
                        () -> userRepository.findByAuthId(authId).map(u -> u.getFullName()).orElse(email)
                );
                emailService.sendStaffPasswordResetEmail(email, fullName, resetToken);
            }
            affected++;
        }

        platformAuditService.log(actorAuthId, "ORG_FORCE_PASSWORD_RESET_EXECUTED", "Organisation", String.valueOf(org.getId()),
                "affectedUsers=" + affected + ", skippedUsers=" + skipped + ", reason=" + (reason == null ? "" : reason));
        return new ExecutionSummary(affected, skipped);
    }

    private void refreshTenantCache(Organisation org) {
        tenantDirectoryService.evictCache();
        if (org.getSchemaName() != null) {
            tenantSchemaHealthService.evict(org.getSchemaName());
        }
    }

    private void revokeOrgSessions(Long orgId) {
        userOrganisationRepository.findByOrganisation_Id(orgId).forEach(link -> {
            Long authId = link.getAuth() != null ? link.getAuth().getId() : null;
            if (authId != null) {
                tokenBlacklistService.blacklistAllUserTokens(authId);
            }
        });
    }

    @Scheduled(fixedDelayString = "${superadmin.termination.purge-ms:60000}")
    @Transactional
    public void processPendingPurgeJobs() {
        if (!isPurgeEnabled()) {
            return;
        }
        processPendingPurgeJobsInternal();
    }

    @Transactional
    public void runPurgeJobsNow() {
        processPendingPurgeJobsInternal();
    }

    public void setPurgeEnabledOverride(Boolean enabled) {
        this.purgeEnabledOverride = enabled;
    }

    public Boolean getPurgeEnabledOverride() {
        return purgeEnabledOverride;
    }

    public boolean isPurgeEnabled() {
        return purgeEnabledOverride != null ? purgeEnabledOverride : purgeEnabled;
    }

    private void processPendingPurgeJobsInternal() {
        Instant now = Instant.now();
        List<PlatformPurgeJob> due = platformPurgeJobRepository
                .findByStatusAndScheduledAtLessThanEqualOrderByScheduledAtAsc("pending", now);
        for (PlatformPurgeJob job : due) {
            String lockKey = "purge-job:" + job.getId();
            boolean locked = acquireLock(lockKey);
            if (!locked) {
                continue;
            }
            try {
                Long orgId = job.getOrganisation().getId();
                purgeOrganisationData(job);
                platformAuditService.log(job.getCreatedByAuthId(), "ORGANISATION_PURGED", "Organisation", String.valueOf(orgId),
                        "purgeJobId=" + job.getId());
            } catch (Exception ex) {
                job.setStatus("failed");
                job.setErrorMessage(ex.getMessage());
                job.setUpdatedAt(Instant.now());
                platformPurgeJobRepository.save(job);
            } finally {
                releaseLock(lockKey);
            }
        }
    }

    private void purgeOrganisationData(PlatformPurgeJob job) {
        Long orgId = job.getOrganisation().getId();
        Organisation org = requireOrganisation(orgId);
        String schemaName = org.getSchemaName();

        revokeOrgSessions(orgId);
        cleanupPublicOrganisationData(orgId);
        archiveTenantAuditLogs(orgId, schemaName);
        dropTenantSchema(schemaName);
        cleanupAuthAndRoles(orgId);

        organisationRepository.deleteById(orgId);
        organisationRepository.flush();
        tenantDirectoryService.evictCache();
        if (schemaName != null && !schemaName.isBlank()) {
            tenantSchemaHealthService.evict(schemaName);
        }
    }

    private void cleanupPublicOrganisationData(Long orgId) {
        cleanupSubscriptionGraph(orgId);
        jdbcTemplate.update("DELETE FROM public.platform_password_reset_jobs WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.platform_backup_jobs WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.platform_impersonation_sessions WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.platform_partner_access WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.billing_contacts WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.billing_notification_logs WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.dunning_policies WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.feature_rollout_rules WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.user_organisation_access_blocks WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.login_attempts WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.auth_identity_roles WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("DELETE FROM public.user_organisations WHERE organisation_id = ?", orgId);
    }

    private void cleanupSubscriptionGraph(Long orgId) {
        jdbcTemplate.update("""
                DELETE FROM public.org_addon_billing_line_items line
                USING public.org_feature_purchases purchase, public.org_subscriptions sub
                WHERE line.org_feature_purchase_id = purchase.id
                  AND purchase.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.org_addon_billing_line_items line
                USING public.org_subscriptions sub
                WHERE line.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("DELETE FROM public.org_addon_billing_line_items WHERE organisation_id = ?", orgId);
        jdbcTemplate.update("""
                DELETE FROM public.invoice_adjustments adj
                USING public.invoices inv, public.org_subscriptions sub
                WHERE adj.invoice_id = inv.id
                  AND inv.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.invoice_disputes dis
                USING public.invoices inv, public.org_subscriptions sub
                WHERE dis.invoice_id = inv.id
                  AND inv.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.feature_usage fu
                USING public.org_subscriptions sub
                WHERE fu.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.invoices inv
                USING public.org_subscriptions sub
                WHERE inv.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.org_feature_purchases purchase
                USING public.org_subscriptions sub
                WHERE purchase.subscription_id = sub.id
                  AND sub.organisation_id = ?
                """, orgId);
        jdbcTemplate.update("DELETE FROM public.org_subscriptions WHERE organisation_id = ?", orgId);
    }

    private void cleanupAuthAndRoles(Long orgId) {
        jdbcTemplate.update("""
                UPDATE public.auth_identities ai
                SET organisation_id = NULL
                WHERE ai.organisation_id = ?
                  AND EXISTS (SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id)
                """, orgId);

        jdbcTemplate.update("""
                DELETE FROM public.user_organisation_access_blocks blocks
                WHERE EXISTS (
                    SELECT 1
                    FROM public.auth_identities ai
                    WHERE ai.id = blocks.auth_id
                      AND ai.organisation_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id
                      )
                )
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.auth_identity_roles air
                WHERE EXISTS (
                    SELECT 1
                    FROM public.auth_identities ai
                    WHERE ai.id = air.auth_id
                      AND ai.organisation_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id
                      )
                )
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.auth_sessions sess
                WHERE EXISTS (
                    SELECT 1
                    FROM public.auth_identities ai
                    WHERE ai.id = sess.auth_id
                      AND ai.organisation_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id
                      )
                )
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.login_attempts la
                WHERE EXISTS (
                    SELECT 1
                    FROM public.auth_identities ai
                    WHERE ai.id = la.auth_id
                      AND ai.organisation_id = ?
                      AND NOT EXISTS (
                          SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id
                      )
                )
                """, orgId);
        jdbcTemplate.update("""
                DELETE FROM public.auth_identities ai
                WHERE ai.organisation_id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM public.user_organisations uo WHERE uo.auth_id = ai.id
                  )
                """, orgId);

        jdbcTemplate.update("""
                DELETE FROM public.role_permissions rp
                WHERE EXISTS (
                    SELECT 1 FROM public.roles r
                    WHERE r.id = rp.role_id
                      AND r.organisation_id = ?
                )
                """, orgId);
        jdbcTemplate.update("DELETE FROM public.roles WHERE organisation_id = ?", orgId);
    }

    private void archiveTenantAuditLogs(Long orgId, String schemaName) {
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return;
        }
        if (!SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalStateException("Unsafe tenant schema name for audit archive: " + schemaName);
        }
        try {
            Long rowCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM \"" + schemaName + "\".audit_logs",
                    Long.class);
            long count = rowCount != null ? rowCount : 0L;
            if (count == 0) {
                jdbcTemplate.update(
                        "INSERT INTO public.tenant_audit_archive (org_id, tenant_schema, row_count, payload) VALUES (?, ?, 0, '[]'::jsonb)",
                        orgId,
                        schemaName);
                log.info("Tenant audit archive: no rows for org {} schema {}", orgId, schemaName);
                return;
            }
            jdbcTemplate.update("""
                    INSERT INTO public.tenant_audit_archive (org_id, tenant_schema, row_count, payload)
                    SELECT ?, ?, COUNT(*)::bigint, COALESCE(jsonb_agg(to_jsonb(t)), '[]'::jsonb)
                      FROM (SELECT * FROM "%s".audit_logs ORDER BY id) t
                    """.formatted(schemaName),
                    orgId,
                    schemaName);
            log.info("Archived {} tenant audit log rows for org {} before schema drop", count, orgId);
            platformAuditService.log(null, "TENANT_AUDIT_ARCHIVED", "Organisation", String.valueOf(orgId),
                    "schema=" + schemaName + ", rowCount=" + count);
        } catch (Exception ex) {
            log.error("Failed to archive tenant audit logs for org {} schema {}: {}", orgId, schemaName, ex.getMessage(), ex);
            platformAuditService.log(null, "TENANT_AUDIT_ARCHIVE_FAILED", "Organisation", String.valueOf(orgId),
                    "schema=" + schemaName + ", error=" + ex.getMessage());
        }
    }

    private void dropTenantSchema(String schemaName) {
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return;
        }
        if (!SAFE_SCHEMA.matcher(schemaName).matches()) {
            throw new IllegalStateException("Unsafe tenant schema name for purge: " + schemaName);
        }
        jdbcTemplate.execute("DROP SCHEMA IF EXISTS \"" + schemaName + "\" CASCADE");
    }

    @Scheduled(fixedDelayString = "${superadmin.termination.warning-ms:3600000}")
    @Transactional
    public void sendTerminationWarnings() {
        if (!isPurgeWarningEnabled()) {
            return;
        }
        sendTerminationWarningsInternal();
    }

    @Transactional
    public void runTerminationWarningsNow() {
        sendTerminationWarningsInternal();
    }

    public void setPurgeWarningEnabledOverride(Boolean enabled) {
        this.purgeWarningEnabledOverride = enabled;
    }

    public Boolean getPurgeWarningEnabledOverride() {
        return purgeWarningEnabledOverride;
    }

    public boolean isPurgeWarningEnabled() {
        return purgeWarningEnabledOverride != null ? purgeWarningEnabledOverride : purgeWarningEnabled;
    }

    private void sendTerminationWarningsInternal() {
        Instant now = Instant.now();
        Instant from = now.plus(6, java.time.temporal.ChronoUnit.DAYS);
        Instant to = now.plus(7, java.time.temporal.ChronoUnit.DAYS).plus(1, java.time.temporal.ChronoUnit.HOURS);
        List<PlatformPurgeJob> upcoming = platformPurgeJobRepository
                .findByStatusAndWarningSentAtIsNullAndScheduledAtBetweenOrderByScheduledAtAsc("pending", from, to);
        for (PlatformPurgeJob job : upcoming) {
            String lockKey = "purge-warning-job:" + job.getId();
            if (!acquireLock(lockKey)) {
                continue;
            }
            try {
                if (job.getWarningSentAt() != null) {
                    continue;
                }
                superAdminNotificationService.createJob(
                        "TARGETED",
                        "Termination warning",
                        "Organisation " + job.getOrganisation().getName() + " is scheduled for purge on " + job.getScheduledAt(),
                        "email",
                        now,
                        Map.of("organisationIds", List.of(job.getOrganisation().getId())),
                        job.getCreatedByAuthId()
                );
                Instant warningSentAt = Instant.now();
                job.setWarningSentAt(warningSentAt);
                job.setUpdatedAt(warningSentAt);
                platformPurgeJobRepository.save(job);
            } finally {
                releaseLock(lockKey);
            }
        }
    }

    private boolean acquireLock(String key) {
        if (redisTemplate.isEmpty()) {
            return redisFailOpen;
        }
        try {
            Boolean ok = redisTemplate.get().opsForValue().setIfAbsent(key, "1", 120, TimeUnit.SECONDS);
            return Boolean.TRUE.equals(ok);
        } catch (Exception ex) {
            if (redisFailOpen) {
                // fallback when redis is unavailable: process in single node best effort
                return true;
            }
            throw ex;
        }
    }

    private void releaseLock(String key) {
        if (redisTemplate.isEmpty()) {
            return;
        }
        try {
            redisTemplate.get().delete(key);
        } catch (Exception ex) {
            if (!redisFailOpen) {
                throw ex;
            }
        }
    }

    public record PasswordResetActionResult(String status, Instant effectiveAt, Long jobId, int affectedUsers, int skippedUsers) {
    }

    private record ExecutionSummary(int affectedUsers, int skippedUsers) {
    }
}
