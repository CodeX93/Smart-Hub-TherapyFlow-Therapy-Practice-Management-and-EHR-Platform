package com.smart.therapy.flow.migration.clienthub;

import com.smart.therapy.flow.billing.entity.Service;
import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.SourceServiceRecord;
import com.smart.therapy.flow.migration.clienthub.ClientHubSourceInventoryService.TargetInventory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
@Slf4j
public class ClientHubServiceExecuteService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final JdbcTemplate jdbcTemplate;
    private final ServiceRepository serviceRepository;

    public ServiceExecuteResult execute(List<SourceServiceRecord> sourceServices, TargetInventory target) {
        if (!target.resolved()) {
            throw new IllegalStateException("Exactly one target organisation is required for service execution");
        }
        return tenantTransactionExecutor.executeWrite(target.organisationId(), target.schemaName(),
                () -> executeInTenant(sourceServices, target));
    }

    private ServiceExecuteResult executeInTenant(List<SourceServiceRecord> sourceServices, TargetInventory target) {
        int created = 0;
        int updated = 0;
        int mapped = 0;
        Set<Long> claimedTargetIds = loadMappedTargetIds(target);
        Set<String> claimedCodes = new HashSet<>();

        for (SourceServiceRecord source : sourceServices) {
            Optional<Long> mappedServiceId = resolveMappedServiceId(target, source.legacyServicePk());
            Service service;
            boolean alreadyMapped = mappedServiceId.isPresent();
            if (alreadyMapped) {
                service = mappedServiceId.flatMap(serviceRepository::findById).orElseGet(Service::new);
            } else {
                service = resolveReusableByCode(source, claimedTargetIds).orElseGet(Service::new);
            }
            boolean existing = service.getId() != null;

            String serviceCode = resolveServiceCode(source, service, claimedCodes);
            applyFields(service, source, serviceCode);
            Service saved = serviceRepository.save(service);
            upsertLegacyMapping(target, source, saved.getId());

            claimedTargetIds.add(saved.getId());
            claimedCodes.add(normaliseCode(serviceCode));

            if (existing) {
                updated++;
                if (alreadyMapped) {
                    mapped++;
                }
            } else {
                created++;
            }
        }

        return new ServiceExecuteResult(sourceServices.size(), created, updated, mapped);
    }

    private Optional<Service> resolveReusableByCode(SourceServiceRecord source, Set<Long> claimedTargetIds) {
        String code = source.serviceCode() == null ? "" : source.serviceCode().trim();
        if (code.isEmpty()) {
            return Optional.empty();
        }
        Optional<Service> existing = serviceRepository.findByServiceCode(code);
        if (existing.isEmpty()) {
            return Optional.empty();
        }
        Long existingId = existing.get().getId();
        if (claimedTargetIds.contains(existingId)) {
            log.warn("ClientHubAI service code already claimed by another mapping; creating distinct target. "
                            + "source_service_pk={} service_code={}",
                    source.legacyServicePk(), code);
            return Optional.empty();
        }
        return existing;
    }

    private String resolveServiceCode(SourceServiceRecord source, Service service, Set<String> claimedCodes) {
        String preferred = source.serviceCode() == null ? "" : source.serviceCode().trim();
        if (service.getId() != null && service.getServiceCode() != null && !service.getServiceCode().isBlank()) {
            // Keep existing target code when updating a mapped/matched row.
            return service.getServiceCode();
        }
        if (!preferred.isEmpty() && !claimedCodes.contains(normaliseCode(preferred))
                && serviceRepository.findByServiceCode(preferred).isEmpty()) {
            return preferred;
        }
        if (!preferred.isEmpty() && !claimedCodes.contains(normaliseCode(preferred))
                && service.getId() != null) {
            return preferred;
        }
        String disambiguated = trimTo(preferred + "__ch" + source.legacyServicePk(), 50);
        log.warn("ClientHubAI service code disambiguated for unique mapping. source_service_pk={} service_code={}",
                source.legacyServicePk(), disambiguated);
        return disambiguated;
    }

    private void applyFields(Service service, SourceServiceRecord source, String serviceCode) {
        service.setServiceCode(serviceCode);
        service.setServiceName(trimTo(source.serviceName(), 255));
        service.setDescription(trim(source.description()));
        service.setDuration(source.duration());
        service.setBaseRate(source.baseRate());
        service.setCategory(trimTo(source.category(), 100));
        service.setIsActive(source.active());
        service.setTherapistVisible(source.therapistVisible());
        service.setClientPortalVisible(source.clientPortalVisible());
        service.setIsDeleted(false);
        service.setDeletedAt(null);
        service.setCreatedBy(0L);
        service.setUpdatedBy(0L);
    }

    private Set<Long> loadMappedTargetIds(TargetInventory target) {
        return new HashSet<>(jdbcTemplate.query("""
                SELECT target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = 'services'
                """,
                (rs, rowNum) -> rs.getLong("target_id"),
                target.organisationId()));
    }

    private Optional<Long> resolveMappedServiceId(TargetInventory target, String sourceId) {
        List<Long> ids = jdbcTemplate.query("""
                SELECT target_id
                FROM public.clienthub_legacy_id_mappings
                WHERE organisation_id = ?
                  AND source_system = 'ClientHubAI'
                  AND entity_name = 'services'
                  AND source_id = ?
                LIMIT 1
                """,
                (rs, rowNum) -> rs.getLong("target_id"),
                target.organisationId(),
                sourceId);
        return ids.stream().findFirst();
    }

    private void upsertLegacyMapping(TargetInventory target, SourceServiceRecord source, Long serviceId) {
        jdbcTemplate.update("""
                INSERT INTO public.clienthub_legacy_id_mappings (
                    organisation_id, source_system, entity_name, source_id,
                    target_schema, target_table, target_id, source_checksum_sha256,
                    first_seen_run_id, last_seen_run_id
                )
                VALUES (?, 'ClientHubAI', 'services', ?, ?, 'services', ?, ?, NULL, NULL)
                ON CONFLICT (organisation_id, source_system, entity_name, source_id)
                DO UPDATE SET
                    target_schema = EXCLUDED.target_schema,
                    target_table = EXCLUDED.target_table,
                    target_id = EXCLUDED.target_id,
                    source_checksum_sha256 = EXCLUDED.source_checksum_sha256,
                    updated_at = CURRENT_TIMESTAMP
                """,
                target.organisationId(),
                source.legacyServicePk(),
                target.schemaName(),
                serviceId,
                checksum(source));
    }

    private String checksum(SourceServiceRecord source) {
        return ClientHubIdentifier.sha256Hex(String.join("|",
                source.legacyServicePk(),
                source.serviceCode(),
                source.serviceName(),
                source.duration().toString(),
                source.baseRate().toPlainString()));
    }

    private String normaliseCode(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
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

    public record ServiceExecuteResult(int sourceServices, int created, int updated, int mapped) {
    }
}
