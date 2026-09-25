package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import com.smart.therapy.flow.organisation.service.TenantStaffProfileBootstrapService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class SuperAdminTenantStaffBootstrapService {

    private final OrganisationRepository organisationRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final TenantStaffProfileBootstrapService tenantStaffProfileBootstrapService;
    private final PlatformAuditService platformAuditService;

    @Transactional
    public BootstrapResult bootstrapOne(Long organisationId, Long actorAuthId) {
        Organisation organisation = organisationRepository.findById(organisationId).orElse(null);
        if (organisation == null) {
            return BootstrapResult.error(organisationId, null, "Organisation not found");
        }
        String schemaName = organisation.getSchemaName();
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return BootstrapResult.error(organisationId, schemaName, "Organisation has no tenant schema");
        }
        if (!tenantSchemaHealthService.schemaExists(schemaName)) {
            return BootstrapResult.error(organisationId, schemaName, "Tenant schema is missing");
        }

        int created;
        try {
            created = tenantStaffProfileBootstrapService.ensureStaffProfiles(organisationId, schemaName, organisation.getTimezone());
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            String normalized = msg.toLowerCase(Locale.ROOT);
            if (normalized.contains("relation \"users\" does not exist") || normalized.contains("relation 'users' does not exist")) {
                return BootstrapResult.error(organisationId, schemaName, "Tenant schema is missing users table; run tenant migrations for this organisation");
            }
            if (normalized.contains("tenant schema") || normalized.contains("schema")) {
                return BootstrapResult.error(organisationId, schemaName, "Tenant schema is not ready: " + msg);
            }
            return BootstrapResult.error(organisationId, schemaName, "Bootstrap failed: " + msg);
        }
        platformAuditService.log(
                actorAuthId,
                "TENANT_STAFF_BOOTSTRAP_RUN",
                "Organisation",
                String.valueOf(organisationId),
                "schema=" + schemaName + ", created=" + created
        );
        return BootstrapResult.success(organisationId, schemaName, created);
    }

    @Transactional
    public BootstrapBatchResult bootstrapAll(int limit, Long actorAuthId) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        List<Organisation> organisations = organisationRepository.findAll().stream()
                .filter(org -> org.getSchemaName() != null
                        && !org.getSchemaName().isBlank()
                        && !"public".equalsIgnoreCase(org.getSchemaName()))
                .sorted(Comparator.comparing(Organisation::getId))
                .limit(safeLimit)
                .toList();

        List<BootstrapResult> results = new ArrayList<>();
        int processed = 0;
        int success = 0;
        int failed = 0;
        int createdTotal = 0;

        for (Organisation organisation : organisations) {
            processed++;
            BootstrapResult result = bootstrapOne(organisation.getId(), actorAuthId);
            results.add(result);
            if (result.success()) {
                success++;
                createdTotal += result.created();
            } else {
                failed++;
            }
        }

        platformAuditService.log(
                actorAuthId,
                "TENANT_STAFF_BOOTSTRAP_BATCH_RUN",
                "Organisation",
                "BATCH",
                "processed=" + processed + ", success=" + success + ", failed=" + failed + ", created=" + createdTotal
        );

        return new BootstrapBatchResult(processed, success, failed, createdTotal, results);
    }

    public record BootstrapResult(
            boolean success,
            Long organisationId,
            String schemaName,
            Integer created,
            String error
    ) {
        static BootstrapResult success(Long organisationId, String schemaName, Integer created) {
            return new BootstrapResult(true, organisationId, schemaName, created, null);
        }

        static BootstrapResult error(Long organisationId, String schemaName, String error) {
            return new BootstrapResult(false, organisationId, schemaName, 0, error);
        }
    }

    public record BootstrapBatchResult(
            int processed,
            int success,
            int failed,
            int created,
            List<BootstrapResult> results
    ) {
    }
}
