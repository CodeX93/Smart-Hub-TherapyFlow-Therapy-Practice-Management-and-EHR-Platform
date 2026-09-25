package com.smart.therapy.flow.unit.system;

import com.smart.therapy.flow.system.service.SystemOptionCategories;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("SystemOptionCategories")
class SystemOptionCategoriesTest {

    @Test
    @DisplayName("categoryLookupKeys includes alias for session_mode and session_modes")
    void sessionModeAliases() {
        assertThat(SystemOptionCategories.categoryLookupKeys(SystemOptionCategories.SESSION_MODE))
                .containsExactly(SystemOptionCategories.SESSION_MODE, SystemOptionCategories.SESSION_MODES);
        assertThat(SystemOptionCategories.categoryLookupKeys(SystemOptionCategories.SESSION_MODES))
                .containsExactly(SystemOptionCategories.SESSION_MODES, SystemOptionCategories.SESSION_MODE);
    }

    @Test
    @DisplayName("categoryLookupKeys includes alias for service_type and service_types")
    void serviceTypeAliases() {
        assertThat(SystemOptionCategories.categoryLookupKeys(SystemOptionCategories.SERVICE_TYPE))
                .contains(SystemOptionCategories.SERVICE_TYPE, SystemOptionCategories.SERVICE_TYPES);
    }

    @Test
    @DisplayName("unknown category returns single lookup key")
    void unknownCategoryReturnsSingleKey() {
        assertThat(SystemOptionCategories.categoryLookupKeys("client_status"))
                .containsExactly("client_status");
    }
}
