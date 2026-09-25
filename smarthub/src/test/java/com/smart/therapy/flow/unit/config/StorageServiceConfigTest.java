package com.smart.therapy.flow.unit.config;

import com.smart.therapy.flow.common.config.StorageServiceConfig;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StorageServiceConfigTest {

    @Test
    void productionRejectsLocalStorage() {
        StorageServiceConfig config = config("prod", "local");

        assertThatThrownBy(config::validateStorageConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production storage must use Azure Blob Storage");
    }

    @Test
    void productionAcceptsAzureStorage() {
        StorageServiceConfig config = config("prod", "azure");

        assertThatCode(config::validateStorageConfiguration).doesNotThrowAnyException();
    }

    @Test
    void nonProductionMayUseLocalStorage() {
        StorageServiceConfig config = config("test", "local");

        assertThatCode(config::validateStorageConfiguration).doesNotThrowAnyException();
    }

    @Test
    void invalidStorageTypeAlwaysFails() {
        StorageServiceConfig config = config("test", "unknown");

        assertThatThrownBy(config::validateStorageConfiguration)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid storage type");
    }

    private StorageServiceConfig config(String profile, String storageType) {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles(profile);
        StorageServiceConfig config = new StorageServiceConfig(environment);
        ReflectionTestUtils.setField(config, "storageType", storageType);
        return config;
    }
}
