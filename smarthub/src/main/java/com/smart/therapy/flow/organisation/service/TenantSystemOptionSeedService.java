package com.smart.therapy.flow.organisation.service;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions.SeedCategory;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions.SeedOption;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Seeds tenant-scoped system option categories and options after provisioning.
 * Idempotent: existing categories/options are left unchanged.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TenantSystemOptionSeedService {

    private final TenantTransactionExecutor tenantTransactionExecutor;
    private final OptionCategoryRepository categoryRepository;
    private final SystemOptionRepository optionRepository;

    public int seedDefaults(Long organisationId, String schemaName) {
        if (organisationId == null || schemaName == null || schemaName.isBlank() || "public".equalsIgnoreCase(schemaName)) {
            return 0;
        }

        Integer seeded = tenantTransactionExecutor.executeWrite(organisationId, schemaName, () -> {
            boolean catalogEmpty = categoryRepository.count() == 0;
            if (catalogEmpty) {
                log.info("System option catalog empty for org {} (schema {}); loading defaults", organisationId, schemaName);
            }
            int created = 0;
            for (SeedCategory seedCategory : SystemOptionSeedDefinitions.defaultTenantCategories()) {
                created += seedCategoryIfMissing(seedCategory);
            }
            return created;
        });
        if (seeded != null && seeded > 0) {
            log.info("Seeded {} default system option record(s) for org {} (schema {})", seeded, organisationId, schemaName);
        }
        return seeded != null ? seeded : 0;
    }

    private int seedCategoryIfMissing(SeedCategory seedCategory) {
        OptionCategory category = categoryRepository.findByCategoryKey(seedCategory.categoryKey()).orElse(null);
        int created = 0;
        if (category == null) {
            category = categoryRepository.save(OptionCategory.builder()
                    .categoryKey(seedCategory.categoryKey())
                    .categoryName(seedCategory.categoryName())
                    .description(seedCategory.description())
                    .isSystem(seedCategory.isSystem())
                    .isActive(seedCategory.isActive())
                    .build());
            created++;
        }

        List<SystemOption> existingOptions = optionRepository.findByCategoryId(category.getId());
        for (SeedOption seedOption : seedCategory.options()) {
            String optionKey = seedOption.optionKey();
            boolean exists = existingOptions.stream()
                    .anyMatch(option -> optionKey.equalsIgnoreCase(option.getOptionKey()));
            if (exists) {
                continue;
            }
            optionRepository.save(SystemOption.builder()
                    .category(category)
                    .optionKey(optionKey)
                    .optionLabel(seedOption.optionLabel())
                    .sortOrder(seedOption.sortOrder())
                    .isDefault(seedOption.isDefault())
                    .isSystem(seedOption.isSystem())
                    .isActive(seedOption.isActive())
                    .price(seedOption.price() != null ? seedOption.price() : BigDecimal.ZERO)
                    .build());
            created++;
        }
        return created;
    }
}
