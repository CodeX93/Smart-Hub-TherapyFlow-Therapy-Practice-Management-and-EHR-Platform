package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.common.exception.StoryApiException;
import com.smart.therapy.flow.organisation.entity.PlatformAuditLog;
import com.smart.therapy.flow.organisation.repository.PlatformAuditLogRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantDirectoryService;
import com.smart.therapy.flow.organisation.service.TenantMigrationScheduler;
import com.smart.therapy.flow.organisation.service.TenantSchemaIntegrityChecker;
import com.smart.therapy.flow.superadmin.dto.AuditLogEntryResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminJobResponse;
import com.smart.therapy.flow.superadmin.dto.SuperAdminJobRunResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class SuperAdminJobAdminService {

    private final SuperAdminNotificationService notificationService;
    private final SuperAdminOperationsService operationsService;
    private final SuperAdminImpersonationService impersonationService;
    private final TenantMigrationScheduler tenantMigrationScheduler;
    private final TenantSchemaIntegrityChecker tenantSchemaIntegrityChecker;
    private final TenantDirectoryService tenantDirectoryService;
    private final PlatformAuditService platformAuditService;
    private final PlatformAuditLogRepository platformAuditLogRepository;

    private static final List<String> JOB_AUDIT_ACTIONS = List.of(
            "JOB_RUN_NOW",
            "JOB_RUN_ALL",
            "JOB_OVERRIDE_UPDATED"
    );

    @Value("${superadmin.notifications.dispatch-ms:30000}")
    private String notificationsSchedule;
    @Value("${superadmin.backups.dispatch-ms:45000}")
    private String backupsSchedule;
    @Value("${superadmin.password-reset.dispatch-ms:45000}")
    private String passwordResetSchedule;
    @Value("${superadmin.termination.purge-ms:60000}")
    private String purgeSchedule;
    @Value("${superadmin.termination.warning-ms:3600000}")
    private String purgeWarningSchedule;
    @Value("${superadmin.impersonation.cleanup-ms:60000}")
    private String impersonationCleanupSchedule;
    @Value("${tenant.migration.fixed-delay-ms:120000}")
    private String tenantMigrationSchedule;
    @Value("${tenant.integrity-check.interval-ms:600000}")
    private String integrityCheckSchedule;
    @Value("${tenant.directory.refresh-ms:60000}")
    private String tenantDirectorySchedule;

    private final Map<String, JobDefinition> jobDefinitions = new ConcurrentHashMap<>();
    private final Map<String, Instant> lastRunAt = new ConcurrentHashMap<>();

    @PostConstruct
    void init() {
        register(new JobDefinition(
                "notifications_dispatch",
                "Dispatch queued platform notifications",
                () -> notificationsSchedule,
                notificationService::isNotificationsEnabled,
                notificationService::getNotificationsEnabledOverride,
                notificationService::setNotificationsEnabledOverride,
                notificationService::runNotificationsDispatchNow
        ));
        register(new JobDefinition(
                "backups_dispatch",
                "Process queued tenant backups",
                () -> backupsSchedule,
                operationsService::isBackupsEnabled,
                operationsService::getBackupsEnabledOverride,
                operationsService::setBackupsEnabledOverride,
                operationsService::runBackupsNow
        ));
        register(new JobDefinition(
                "password_reset_dispatch",
                "Dispatch scheduled organisation password resets",
                () -> passwordResetSchedule,
                operationsService::isPasswordResetEnabled,
                operationsService::getPasswordResetEnabledOverride,
                operationsService::setPasswordResetEnabledOverride,
                operationsService::runPasswordResetDispatchNow
        ));
        register(new JobDefinition(
                "purge_dispatch",
                "Process pending termination purge jobs",
                () -> purgeSchedule,
                operationsService::isPurgeEnabled,
                operationsService::getPurgeEnabledOverride,
                operationsService::setPurgeEnabledOverride,
                operationsService::runPurgeJobsNow
        ));
        register(new JobDefinition(
                "purge_warning",
                "Send termination warning notifications",
                () -> purgeWarningSchedule,
                operationsService::isPurgeWarningEnabled,
                operationsService::getPurgeWarningEnabledOverride,
                operationsService::setPurgeWarningEnabledOverride,
                operationsService::runTerminationWarningsNow
        ));
        register(new JobDefinition(
                "impersonation_cleanup",
                "Expire stale impersonation sessions",
                () -> impersonationCleanupSchedule,
                impersonationService::isImpersonationCleanupEnabled,
                impersonationService::getImpersonationCleanupEnabledOverride,
                impersonationService::setImpersonationCleanupEnabledOverride,
                impersonationService::runImpersonationCleanupNow
        ));
        register(new JobDefinition(
                "tenant_migration",
                "Migrate queued tenants in batches",
                () -> tenantMigrationSchedule,
                tenantMigrationScheduler::isMigrationEnabled,
                tenantMigrationScheduler::getMigrationEnabledOverride,
                tenantMigrationScheduler::setMigrationEnabledOverride,
                tenantMigrationScheduler::runMigrationBatchNow
        ));
        register(new JobDefinition(
                "tenant_integrity_check",
                "Verify tenant schema integrity",
                () -> integrityCheckSchedule,
                tenantSchemaIntegrityChecker::isIntegrityCheckEnabled,
                tenantSchemaIntegrityChecker::getIntegrityCheckEnabledOverride,
                tenantSchemaIntegrityChecker::setIntegrityCheckEnabledOverride,
                tenantSchemaIntegrityChecker::runIntegrityCheckNow
        ));
        register(new JobDefinition(
                "tenant_directory_refresh",
                "Refresh tenant routing directory",
                () -> tenantDirectorySchedule,
                tenantDirectoryService::isDirectoryRefreshEnabled,
                tenantDirectoryService::getDirectoryRefreshEnabledOverride,
                tenantDirectoryService::setDirectoryRefreshEnabledOverride,
                tenantDirectoryService::runDirectoryRefreshNow
        ));
    }

    public List<SuperAdminJobResponse> listJobs() {
        List<SuperAdminJobResponse> rows = new ArrayList<>();
        for (JobDefinition def : jobDefinitions.values()) {
            SuperAdminJobResponse row = new SuperAdminJobResponse();
            row.setKey(def.key());
            row.setDescription(def.description());
            row.setSchedule(def.scheduleSupplier().get());
            row.setEnabled(def.enabledSupplier().get());
            row.setOverrideEnabled(def.overrideSupplier().get());
            row.setLastRunAt(lastRunAt.get(def.key()));
            rows.add(row);
        }
        rows.sort(java.util.Comparator.comparing(SuperAdminJobResponse::getKey));
        return rows;
    }

    public SuperAdminJobRunResponse runNow(String key) {
        return runNow(key, null);
    }

    public SuperAdminJobRunResponse runNow(String key, Long actorAuthId) {
        JobDefinition def = jobDefinitions.get(key);
        if (def == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Unknown job: " + key);
        }
        def.runNow().run();
        Instant now = Instant.now();
        lastRunAt.put(key, now);
        SuperAdminJobRunResponse response = new SuperAdminJobRunResponse();
        response.setKey(key);
        response.setStatus("executed");
        response.setRanAt(now);
        platformAuditService.log(actorAuthId, "JOB_RUN_NOW", "ScheduledJob", key, "status=executed");
        return response;
    }

    public List<SuperAdminJobRunResponse> runAllNow(Long actorAuthId) {
        List<SuperAdminJobRunResponse> results = new ArrayList<>();
        for (String key : jobDefinitions.keySet().stream().sorted().toList()) {
            results.add(runNow(key, actorAuthId));
        }
        platformAuditService.log(actorAuthId, "JOB_RUN_ALL", "ScheduledJob", "all", "count=" + results.size());
        return results;
    }

    public SuperAdminJobResponse setEnabledOverride(String key, Boolean enabled, Long actorAuthId) {
        JobDefinition def = jobDefinitions.get(key);
        if (def == null) {
            throw new StoryApiException(HttpStatus.NOT_FOUND, "JOB_NOT_FOUND", "Unknown job: " + key);
        }
        def.overrideSetter().accept(enabled);
        platformAuditService.log(actorAuthId, "JOB_OVERRIDE_UPDATED", "ScheduledJob", key, "enabled=" + enabled);
        return listJobs().stream()
                .filter(r -> r.getKey().equals(key))
                .findFirst()
                .orElseThrow();
    }

    public List<AuditLogEntryResponse> listJobAuditLogs(String jobKey, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);
        return platformAuditLogRepository.findAll(
                        org.springframework.data.domain.PageRequest.of(safePage, safeSize,
                                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"))
                )
                .stream()
                .filter(log -> log.getAction() != null && JOB_AUDIT_ACTIONS.contains(log.getAction()))
                .filter(log -> "ScheduledJob".equalsIgnoreCase(log.getResourceType()))
                .filter(log -> jobKey == null || jobKey.isBlank()
                        || (log.getResourceId() != null && log.getResourceId().equalsIgnoreCase(jobKey)))
                .map(this::toAuditLogResponse)
                .toList();
    }

    private void register(JobDefinition def) {
        jobDefinitions.put(def.key(), def);
    }

    private AuditLogEntryResponse toAuditLogResponse(PlatformAuditLog log) {
        AuditLogEntryResponse r = new AuditLogEntryResponse();
        r.setId(log.getId());
        r.setAuthId(log.getAuthId());
        r.setAction(log.getAction());
        r.setResourceType(log.getResourceType());
        r.setResourceId(log.getResourceId());
        r.setDetails(log.getDetails());
        r.setCreatedAt(log.getCreatedAt());
        return r;
    }

    private record JobDefinition(
            String key,
            String description,
            Supplier<String> scheduleSupplier,
            Supplier<Boolean> enabledSupplier,
            Supplier<Boolean> overrideSupplier,
            Consumer<Boolean> overrideSetter,
            Runnable runNow
    ) {
    }
}
