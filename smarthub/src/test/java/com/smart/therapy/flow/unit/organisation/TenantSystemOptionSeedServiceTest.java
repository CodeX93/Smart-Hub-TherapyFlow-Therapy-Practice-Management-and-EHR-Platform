package com.smart.therapy.flow.unit.organisation;

import com.smart.therapy.flow.common.tenant.TenantTransactionExecutor;
import com.smart.therapy.flow.organisation.service.TenantSystemOptionSeedService;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("TenantSystemOptionSeedService")
class TenantSystemOptionSeedServiceTest {

    @Mock
    private TenantTransactionExecutor tenantTransactionExecutor;

    @Mock
    private OptionCategoryRepository categoryRepository;

    @Mock
    private SystemOptionRepository optionRepository;

    @InjectMocks
    private TenantSystemOptionSeedService seedService;

    @Test
    @DisplayName("seeds all categories when catalog is empty after V41 wipe")
    void seedsAllCategoriesWhenCatalogEmpty() {
        when(tenantTransactionExecutor.executeWrite(eq(5L), eq("tenant_test"), any()))
                .thenAnswer(invocation -> {
                    Supplier<?> supplier = invocation.getArgument(2);
                    return supplier.get();
                });
        when(categoryRepository.count()).thenReturn(0L);
        when(categoryRepository.findByCategoryKey(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(OptionCategory.class))).thenAnswer(invocation -> {
            OptionCategory category = invocation.getArgument(0);
            category.setId(1L);
            return category;
        });
        when(optionRepository.findByCategoryId(any())).thenReturn(new ArrayList<>());

        int created = seedService.seedDefaults(5L, "tenant_test");

        int expectedCategories = SystemOptionSeedDefinitions.defaultTenantCategories().size();
        int expectedOptions = SystemOptionSeedDefinitions.defaultTenantCategories().stream()
                .mapToInt(category -> category.options().size())
                .sum();

        assertThat(created).isEqualTo(expectedCategories + expectedOptions);
        verify(categoryRepository, atLeastOnce()).save(any(OptionCategory.class));
    }

    @Test
    @DisplayName("skips invalid schema inputs")
    void skipsInvalidSchemaInputs() {
        assertThat(seedService.seedDefaults(null, "tenant_test")).isZero();
        assertThat(seedService.seedDefaults(1L, null)).isZero();
        assertThat(seedService.seedDefaults(1L, "public")).isZero();
    }
}
