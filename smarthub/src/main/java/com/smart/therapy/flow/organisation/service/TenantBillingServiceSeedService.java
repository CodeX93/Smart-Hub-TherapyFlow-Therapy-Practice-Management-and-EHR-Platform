package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.billing.repository.ServiceRepository;
import com.smart.therapy.flow.billing.service.BillingServiceSeedDefinitions;
import com.smart.therapy.flow.billing.service.BillingServiceSeedDefinitions.SeedService;
import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Seeds tenant-scoped billing services after provisioning when the catalog is empty.
 * Idempotent: existing service codes are left unchanged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantBillingServiceSeedService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final ServiceRepository serviceRepository;

    public int seedDefaults(Long organisationId, String schemaName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }

        Integer seeded = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            if (serviceRepository.count() > 0) {
                return 0;
            }
            log.info("Billing service catalog empty for org {} (schema {}); loading defaults", organisationId, schemaName);
            int created = 0;
            for (SeedService seed : BillingServiceSeedDefinitions.defaultServices()) {
                if (serviceRepository.findByServiceCode(seed.serviceCode()).isPresent()) {
                    continue;
                }
                serviceRepository.save(com.smart.therapy.flow.billing.entity.Service.builder()
                        .serviceCode(seed.serviceCode())
                        .serviceName(seed.serviceName())
                        .description(seed.description())
                        .duration(seed.durationMinutes())
                        .baseRate(seed.baseRate())
                        .category(seed.category())
                        .isActive(true)
                        .therapistVisible(true)
                        .clientPortalVisible(seed.clientPortalVisible())
                        .build());
                created++;
            }
            return created;
        });
        if (seeded != null && seeded > 0) {
            log.info("Seeded {} default billing service(s) for org {} (schema {})", seeded, organisationId, schemaName);
        }
        return seeded != null ? seeded : 0;
    }
}
