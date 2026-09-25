package com.smart.therapy.flow.unit.system;

import com.smart.therapy.flow.system.dto.OptionCategoryResponse;
import com.smart.therapy.flow.system.entity.OptionCategory;
import com.smart.therapy.flow.system.entity.SystemOption;
import com.smart.therapy.flow.system.repository.OptionCategoryRepository;
import com.smart.therapy.flow.system.repository.SystemOptionRepository;
import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionResolverService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SystemOptionResolverService")
class SystemOptionResolverServiceTest {

    @Mock
    private OptionCategoryRepository categoryRepository;

    @Mock
    private SystemOptionRepository optionRepository;

    @InjectMocks
    private SystemOptionResolverService resolverService;

    private OptionCategory legacySessionModesCategory;
    private SystemOption inPersonOption;

    @BeforeEach
    void setUp() {
        legacySessionModesCategory = OptionCategory.builder()
                .categoryKey(SystemOptionCategories.SESSION_MODES)
                .categoryName("Session Modes")
                .isSystem(true)
                .isActive(true)
                .build();
        legacySessionModesCategory.setId(10L);

        inPersonOption = SystemOption.builder()
                .category(legacySessionModesCategory)
                .optionKey("in_person")
                .optionLabel("In-Person")
                .sortOrder(1)
                .isDefault(true)
                .isSystem(true)
                .isActive(true)
                .build();
    }

    @Test
    @DisplayName("resolves session_mode via legacy session_modes alias")
    void resolvesSessionModeViaAlias() {
        when(categoryRepository.findByCategoryKey(SystemOptionCategories.SESSION_MODE))
                .thenReturn(Optional.empty());
        when(categoryRepository.findByCategoryKey(SystemOptionCategories.SESSION_MODES))
                .thenReturn(Optional.of(legacySessionModesCategory));
        when(optionRepository.findByCategoryId(10L)).thenReturn(List.of(inPersonOption));

        OptionCategoryResponse response = resolverService.resolveCategoryWithOptions(SystemOptionCategories.SESSION_MODE);

        assertThat(response.getOptions()).hasSize(1);
        assertThat(response.getOptions().get(0).getOptionKey()).isEqualTo("in_person");
    }

    @Test
    @DisplayName("resolveOptionKey matches option key and label case-insensitively")
    void resolveOptionKeyMatchesKeyAndLabel() {
        when(categoryRepository.findByCategoryKey(SystemOptionCategories.SESSION_MODE))
                .thenReturn(Optional.of(legacySessionModesCategory));
        when(optionRepository.findByCategoryId(10L)).thenReturn(List.of(inPersonOption));

        assertThat(resolverService.resolveOptionKey(SystemOptionCategories.SESSION_MODE, "in_person"))
                .isEqualTo("in_person");
        assertThat(resolverService.resolveOptionKey(SystemOptionCategories.SESSION_MODE, "In-Person"))
                .isEqualTo("in_person");
    }

    @Test
    @DisplayName("resolveOptionKey falls back to enum binding when category is empty")
    void resolveOptionKeyFallsBackToEnumBinding() {
        when(categoryRepository.findByCategoryKey(SystemOptionCategories.SESSION_STATUS))
                .thenReturn(Optional.empty());

        assertThat(resolverService.resolveOptionKey(SystemOptionCategories.SESSION_STATUS, "scheduled"))
                .isEqualTo("scheduled");
        assertThat(resolverService.resolveOptionKey(SystemOptionCategories.SESSION_STATUS, "SCHEDULED"))
                .isEqualTo("scheduled");
    }

    @Test
    @DisplayName("all seeded categories are configurable (not workflow-strict)")
    void allCategoriesAreConfigurable() {
        com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions.defaultTenantCategories()
                .forEach(category -> assertThat(
                        com.smart.therapy.flow.system.service.SystemOptionPolicy.isWorkflowStrict(category.categoryKey()))
                        .as("category %s should be configurable", category.categoryKey())
                        .isFalse());
    }
}
