package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.ConfigKeyProvider;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigKeyProviderTest {

    private static final String MASTER_KEY =
            "test-master-key-with-at-least-thirty-two-characters";

    @Test
    void refusesToStartUnderProdProfile() {
        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("prod");

        assertThatThrownBy(() -> new ConfigKeyProvider(env, MASTER_KEY, "primary", "", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("prod");
    }

    @Test
    void derivesDistinctDekAndSearchHmacKeys() {
        ConfigKeyProvider provider = new ConfigKeyProvider(
                new MockEnvironment(), MASTER_KEY, "primary", "", null);

        assertThat(provider.getCurrentKeyId()).isEqualTo("primary");
        assertThat(provider.getAvailableKeyIds()).containsExactly("primary");
        assertThat(provider.getDataEncryptionKey("primary").getEncoded())
                .isNotEqualTo(provider.getSearchHmacKey());
    }

    @Test
    void loadsPreviousKeysIntoRing() {
        ConfigKeyProvider provider = new ConfigKeyProvider(
                new MockEnvironment(),
                "new-master-key-with-at-least-thirty-two-characters",
                "new",
                "old=" + MASTER_KEY,
                null);

        assertThat(provider.getAvailableKeyIds()).containsExactlyInAnyOrder("new", "old");
        assertThat(provider.getDataEncryptionKey("old")).isNotNull();
    }
}
