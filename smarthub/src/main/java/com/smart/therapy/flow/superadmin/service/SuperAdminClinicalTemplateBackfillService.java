package com.smart.therapy.flow.superadmin.service;

import com.smart.therapy.flow.organisation.entity.Organisation;
import com.smart.therapy.flow.organisation.repository.OrganisationRepository;
import com.smart.therapy.flow.organisation.service.PlatformAuditService;
import com.smart.therapy.flow.organisation.service.TenantClinicalTemplateSeedService;
import com.smart.therapy.flow.organisation.service.TenantSchemaHealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Backfills ClientHub-derived clinical templates into existing tenant schemas.
 */
@Service
@RequiredArgsConstructor
public class SuperAdminClinicalTemplateBackfillService {

    private final OrganisationRepository organisationRepository;
    private final TenantSchemaHealthService tenantSchemaHealthService;
    private final TenantClinicalTemplateSeedService tenantClinicalTemplateSeedService;
    private final PlatformAuditService platformAuditService;

    @Transactional
    public BackfillResult backfillOne(Long organisationId, Long actorAuthId) {
        Organisation organisation = organisationRepository.findById(organisationId).orElse(null);
        if (organisation == null) {
            return BackfillResult.error(organisationId, null, "Organisation not found");
        }
        String schemaName = organisation.getSchemaName();
        if (schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return BackfillResult.error(organisationId, schemaName, "Organisation has no tenant schema");
        }
        if (!tenantSchemaHealthService.schemaExists(schemaName)) {
            return BackfillResult.error(organisationId, schemaName, "Tenant schema is missing");
        }

        int created;
        try {
            created = tenantClinicalTemplateSeedService.seedDefaults(organisationId, schemaName);
        } catch (RuntimeException ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            String normalized = msg.toLowerCase(Locale.ROOT);
            if (normalized.contains("relation") && normalized.contains("does not exist")) {
                return BackfillResult.error(organisationId, schemaName,
                        "Tenant schema is missing required tables; run tenant migrations for this organisation");
            }
            return BackfillResult.error(organisationId, schemaName, "Clinical template backfill failed: " + msg);
        }

        platformAuditService.log(
                actorAuthId,
                "TENANT_CLINICAL_TEMPLATE_BACKFILL_RUN",
                "Organisation",
                String.valueOf(organisationId),
                "schema=" + schemaName + ", created=" + created
        );
        return BackfillResult.success(organisationId, schemaName, created);
    }

    @Transactional
    public BackfillBatchResult backfillAll(int limit, Long actorAuthId) {
        int safeLimit = Math.max(1, Math.min(limit, 1000));
        List<Organisation> organisations = organisationRepository.findAll().stream()
                .filter(org -> org.getSchemaName() != null
                        && !org.getSchemaName().isBlank()
                        && !"public".equalsIgnoreCase(org.getSchemaName()))
                .sorted(Comparator.comparing(Organisation::getId))
                .limit(safeLimit)
                .toList();

        List<BackfillResult> results = new ArrayList<>();
        int processed = 0;
        int success = 0;
        int failed = 0;
        int createdTotal = 0;

        for (Organisation organisation : organisations) {
            processed++;
            BackfillResult result = backfillOne(organisation.getId(), actorAuthId);
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
                "TENANT_CLINICAL_TEMPLATE_BACKFILL_BATCH_RUN",
                "Organisation",
                "BATCH",
                "processed=" + processed + ", success=" + success + ", failed=" + failed + ", created=" + createdTotal
        );

        return new BackfillBatchResult(processed, success, failed, createdTotal, results);
    }

    public record BackfillResult(
            boolean success,
            Long organisationId,
            String schemaName,
            Integer created,
            String error
    ) {
        static BackfillResult success(Long organisationId, String schemaName, Integer created) {
            return new BackfillResult(true, organisationId, schemaName, created, null);
        }

        static BackfillResult error(Long organisationId, String schemaName, String error) {
            return new BackfillResult(false, organisationId, schemaName, 0, error);
        }
    }

    public record BackfillBatchResult(
            int processed,
            int success,
            int failed,
            int created,
            List<BackfillResult> results
    ) {
    }
}
