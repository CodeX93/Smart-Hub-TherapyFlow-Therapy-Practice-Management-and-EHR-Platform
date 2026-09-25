package com.smart.therapy.flow.common.config;

import com.smart.therapy.flow.common.service.ConfigKeyProvider;
import com.smart.therapy.flow.common.service.KeyProvider;
import com.smart.therapy.flow.common.service.KeyVaultKeyProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.Arrays;

/**
 * Wires {@link KeyProvider} from {@code app.encryption.provider} ({@code config}|{@code keyvault}).
 */
@Configuration
@Slf4j
public class EncryptionKeyConfig {

    @Bean
    @ConditionalOnProperty(name = "app.encryption.provider", havingValue = "config", matchIfMissing = true)
    public KeyProvider configKeyProvider(
            Environment environment,
            @Value("${app.encryption.master-key:${jasypt.encryptor.password:}}") String masterKey,
            @Value("${app.encryption.key-id:primary}") String keyId,
            @Value("${app.encryption.previous-keys:}") String previousKeys,
            @Value("${app.encryption.search-hmac-key:}") String searchHmacKey
    ) {
        assertProdUsesKeyVault(environment, "config");
        log.info("PHI KeyProvider: config (dev/test only)");
        return new ConfigKeyProvider(environment, masterKey, keyId, previousKeys, searchHmacKey);
    }

    @Bean
    @ConditionalOnProperty(name = "app.encryption.provider", havingValue = "keyvault")
    public KeyProvider keyVaultKeyProvider(
            @Value("${app.encryption.keyvault-uri:${AZURE_KEYVAULT_URI:}}") String vaultUri,
            @Value("${app.encryption.kek-name:${APP_ENCRYPTION_KEK_NAME:}}") String kekName,
            @Value("${app.encryption.search-hmac-key-name:${APP_SEARCH_HMAC_KEY_NAME:}}") String searchHmacKeyName,
            @Value("${app.encryption.key-id:primary}") String keyId,
            @Value("${app.encryption.previous-keys:}") String previousKeys
    ) {
        log.info("PHI KeyProvider: Azure Key Vault");
        return new KeyVaultKeyProvider(vaultUri, kekName, searchHmacKeyName, keyId, previousKeys);
    }

    private static void assertProdUsesKeyVault(Environment environment, String provider) {
        boolean production = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> "prod".equalsIgnoreCase(p) || "production".equalsIgnoreCase(p));
        String appEnv = environment.getProperty("app.env", "");
        if ((production || "prod".equalsIgnoreCase(appEnv) || "production".equalsIgnoreCase(appEnv))
                && !"keyvault".equalsIgnoreCase(provider)) {
            throw new IllegalStateException(
                    "Production requires APP_ENCRYPTION_PROVIDER=keyvault (ConfigKeyProvider is forbidden)");
        }
    }
}
