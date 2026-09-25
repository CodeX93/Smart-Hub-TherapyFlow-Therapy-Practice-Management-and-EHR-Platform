package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantFlywayMigrator;
import com.smart.therapy.flow.organisation.service.TenantMigrationScheduler;
import com.smart.therapy.flow.organisation.service.TenantProvisioningService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SuperAdminTenantOpsFacade {

    private final TenantProvisioningService tenantProvisioningService;
    private final PlatformAuditService platformAuditService;
    private final SuperAdminOperationsService superAdminOperationsService;
    private final TenantMigrationScheduler tenantMigrationScheduler;

    public ProvisionResult queueProvisioning(Long organisationId, Long actorAuthId) {
        try {
            tenantProvisioningService.queueTenantProvisioning(organisationId);
            platformAuditService.log(actorAuthId, "TENANT_PROVISION_QUEUED", "Organisation", String.valueOf(organisationId), "schema migration queued");
            return ProvisionResult.accepted();
        } catch (IllegalArgumentException e) {
            return ProvisionResult.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Provisioning failed for org {}", organisationId, e);
            return ProvisionResult.internalError(e.getMessage());
        }
    }

    /**
     * Queue tenant provisioning and immediately trigger the migration scheduler
     * to start processing without waiting for the next scheduled run.
     */
    public ProvisionResult queueAndTriggerProvisioning(Long organisationId, Long actorAuthId) {
        try {
            tenantProvisioningService.queueTenantProvisioning(organisationId);
            platformAuditService.log(actorAuthId, "TENANT_PROVISION_QUEUED", "Organisation", String.valueOf(organisationId), "schema migration queued and triggered");

            // Immediately trigger the migration scheduler to process this tenant
            log.info("Triggering migration scheduler immediately for organisation {}", organisationId);
            tenantMigrationScheduler.runMigrationBatchNow();

            return ProvisionResult.accepted();
        } catch (IllegalArgumentException e) {
            return ProvisionResult.badRequest(e.getMessage());
        } catch (Exception e) {
            log.error("Provisioning failed for org {}", organisationId, e);
            return ProvisionResult.internalError(e.getMessage());
        }
    }

    /**
     * Synchronous reprovision: template clone (if needed), Flyway migrations, and default seeds —
     * identical to what the background scheduler runs after a successful migrate.
     */
    public ReprovisionResult reprovisionSynchronously(Long organisationId, Long actorAuthId) {
        try {
            var result = tenantProvisioningService.reprovisionTenant(organisationId);
            platformAuditService.log(
                    actorAuthId,
                    "TENANT_REPROVISIONED",
                    "Organisation",
                    String.valueOf(organisationId),
                    "schema version=" + result.version()
                            + ", staff=" + result.seeds().staffProfiles()
                            + ", options=" + result.seeds().systemOptions()
                            + ", library=" + result.seeds().libraryRecords()
                            + ", billing=" + result.seeds().billingServices());
            return ReprovisionResult.success(organisationId, result);
        } catch (IllegalArgumentException e) {
            return ReprovisionResult.badRequest(organisationId, e.getMessage());
        } catch (Exception e) {
            log.error("Reprovision failed for org {}", organisationId, e);
            return ReprovisionResult.internalError(organisationId, e.getMessage());
        }
    }

    public LockResult lockForMaintenance(Long organisationId, Long actorAuthId) {
        try {
            Organisation org = superAdminOperationsService.suspendOrganisation(
                    organisationId,
                    actorAuthId,
                    "Maintenance lock requested by super admin"
            );
            return LockResult.success(org.getStatus());
        } catch (IllegalArgumentException e) {
            return LockResult.error(e.getMessage());
        }
    }

    public BackupResult requestBackup(Long organisationId, Long actorAuthId) {
        try {
            var job = superAdminOperationsService.requestBackup(organisationId, actorAuthId);
            return BackupResult.success(job.getId(), job.getStatus());
        } catch (IllegalArgumentException e) {
            return BackupResult.error(e.getMessage());
        }
    }

    public record ProvisionResult(boolean success, Integer statusCode, String error) {
        public static ProvisionResult accepted() {
            return new ProvisionResult(true, 202, null);
        }
        public static ProvisionResult badRequest(String error) {
            return new ProvisionResult(false, 400, error);
        }
        public static ProvisionResult internalError(String error) {
            return new ProvisionResult(false, 500, error);
        }
    }

    public record ReprovisionResult(
            boolean success,
            Integer statusCode,
            Long organisationId,
            String error,
            TenantFlywayMigrator.TenantMigrationResult migration
    ) {
        public static ReprovisionResult success(Long organisationId, TenantFlywayMigrator.TenantMigrationResult migration) {
            return new ReprovisionResult(true, 200, organisationId, null, migration);
        }
        public static ReprovisionResult badRequest(Long organisationId, String error) {
            return new ReprovisionResult(false, 400, organisationId, error, null);
        }
        public static ReprovisionResult internalError(Long organisationId, String error) {
            return new ReprovisionResult(false, 500, organisationId, error, null);
        }
    }

    public record LockResult(boolean success, String status, String error) {
        public static LockResult success(String status) {
            return new LockResult(true, status, null);
        }
        public static LockResult error(String error) {
            return new LockResult(false, null, error);
        }
    }

    public record BackupResult(boolean success, Long jobId, String status, String error) {
        public static BackupResult success(Long jobId, String status) {
            return new BackupResult(true, jobId, status, null);
        }
        public static BackupResult error(String error) {
            return new BackupResult(false, null, null, error);
        }
    }
}

