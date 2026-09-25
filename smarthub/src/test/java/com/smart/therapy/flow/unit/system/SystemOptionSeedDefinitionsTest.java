package com.smart.therapy.flow.unit.system;

import com.smart.therapy.flow.system.service.SystemOptionCategories;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions.SeedCategory;
import com.smart.therapy.flow.system.service.SystemOptionSeedDefinitions.SeedOption;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemOptionSeedDefinitions")
class SystemOptionSeedDefinitionsTest {

    @Test
    @DisplayName("loads legacy catalog with 24 categories and 174 options")
    void loadsLegacyCatalog() {
        List<SeedCategory> categories = SystemOptionSeedDefinitions.defaultTenantCategories();

        assertThat(categories).hasSize(24);

        int optionCount = categories.stream()
                .mapToInt(category -> category.options() != null ? category.options().size() : 0)
                .sum();
        assertThat(optionCount).isEqualTo(174);
    }

    @Test
    @DisplayName("uses canonical session_mode category key (not session_modes)")
    void usesCanonicalSessionModeKey() {
        List<SeedCategory> categories = SystemOptionSeedDefinitions.defaultTenantCategories();

        assertThat(categories.stream().map(SeedCategory::categoryKey))
                .contains(SystemOptionCategories.SESSION_MODE)
                .doesNotContain(SystemOptionCategories.SESSION_MODES);
    }

    @Test
    @DisplayName("includes all core workflow categories used by application code")
    void includesCoreWorkflowCategories() {
        Set<String> keys = new HashSet<>();
        for (SeedCategory category : SystemOptionSeedDefinitions.defaultTenantCategories()) {
            keys.add(category.categoryKey());
        }

        assertThat(keys).contains(
                SystemOptionCategories.CLIENT_STATUS,
                SystemOptionCategories.CLIENT_STAGE,
                SystemOptionCategories.CLIENT_TYPE,
                SystemOptionCategories.GENDER,
                SystemOptionCategories.MARITAL_STATUS,
                SystemOptionCategories.SERVICE_TYPE,
                SystemOptionCategories.SERVICE_FREQUENCY,
                SystemOptionCategories.EMPLOYMENT_STATUS,
                SystemOptionCategories.EDUCATION_LEVEL,
                SystemOptionCategories.REFERRAL_SOURCES,
                SystemOptionCategories.TASK_STATUS,
                SystemOptionCategories.SESSION_STATUS,
                SystemOptionCategories.SESSION_MODE
        );
    }

    @Test
    @DisplayName("category keys and option keys within a category are unique")
    void keysAreUnique() {
        List<SeedCategory> categories = SystemOptionSeedDefinitions.defaultTenantCategories();

        Set<String> categoryKeys = new HashSet<>();
        for (SeedCategory category : categories) {
            assertThat(categoryKeys.add(category.categoryKey()))
                    .as("duplicate category key: %s", category.categoryKey())
                    .isTrue();

            Set<String> optionKeys = new HashSet<>();
            for (SeedOption option : category.options()) {
                assertThat(optionKeys.add(option.optionKey()))
                        .as("duplicate option key %s in category %s", option.optionKey(), category.categoryKey())
                        .isTrue();
            }
        }
    }

    @Test
    @DisplayName("session_mode options include legacy delivery modes")
    void sessionModeOptionsIncludeLegacyValues() {
        SeedCategory sessionMode = SystemOptionSeedDefinitions.defaultTenantCategories().stream()
                .filter(category -> SystemOptionCategories.SESSION_MODE.equals(category.categoryKey()))
                .findFirst()
                .orElseThrow();

        Set<String> optionKeys = new HashSet<>();
        for (SeedOption option : sessionMode.options()) {
            optionKeys.add(option.optionKey());
        }

        assertThat(optionKeys).contains("in_person", "Online", "phone", "hybrid");
    }
}
