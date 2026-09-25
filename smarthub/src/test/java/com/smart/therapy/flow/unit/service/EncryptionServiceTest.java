package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.service.ConfigKeyProvider;
import com.smart.therapy.flow.common.service.EncryptionService;
import com.smart.therapy.flow.common.service.KeyProvider;
import org.jasypt.encryption.StringEncryptor;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EncryptionServiceTest {

    private static final String MASTER_KEY =
            "test-master-key-with-at-least-thirty-two-characters";

    @Test
    void encryptsDeterministicallyWithStableCiphertext() {
        EncryptionService service = service("primary", "");

        String first = service.encryptDeterministic("Jane Doe", "clients.full_name");
        String second = service.encryptDeterministic("Jane Doe", "clients.full_name");
        String otherPurpose = service.encryptDeterministic("Jane Doe", "client_contacts.contact_value");

        assertThat(first).startsWith("TFENC:v2d:primary:");
        assertThat(second).isEqualTo(first);
        assertThat(otherPurpose).isNotEqualTo(first);
        assertThat(service.decrypt(first)).isEqualTo("Jane Doe");
    }

    @Test
    void encryptsWithRandomizedAuthenticatedCiphertext() {
        EncryptionService service = service("key-2026", "");

        String first = service.encrypt("clinical narrative");
        String second = service.encrypt("clinical narrative");

        assertThat(first).startsWith("TFENC:v2:key-2026:");
        assertThat(second).startsWith("TFENC:v2:key-2026:");
        assertThat(first).isNotEqualTo(second);
        assertThat(service.decrypt(first)).isEqualTo("clinical narrative");
        assertThat(service.decrypt(second)).isEqualTo("clinical narrative");
    }

    @Test
    void returnsCiphertextWhenTamperedSoJpaReadsDoNotFailClosed() {
        EncryptionService service = service("primary", "");
        String encrypted = service.encrypt("protected value");
        char replacement = encrypted.endsWith("A") ? 'B' : 'A';
        String tampered = encrypted.substring(0, encrypted.length() - 1) + replacement;

        // Soft-fail: unavailable/tampered TFENC must not 500 document/client list endpoints.
        assertThat(service.decrypt(tampered)).isEqualTo(tampered);
        assertThat(service.isEncrypted(tampered)).isTrue();
    }

    @Test
    void rejectsDeterministicEncryptWhenBlindOnly() {
        EncryptionService service = new EncryptionService(
                mock(StringEncryptor.class), keyProvider("primary", ""), "blind_only");

        assertThatThrownBy(() -> service.encryptDeterministic("Jane Doe", "clients.full_name"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("blind_only");
    }

    @Test
    void decryptsCiphertextWrittenWithPreviousKey() {
        EncryptionService oldService = service("old", "");
        String encrypted = oldService.encrypt("rotatable value");

        KeyProvider rotated = new ConfigKeyProvider(
                testEnv(),
                "new-master-key-with-at-least-thirty-two-characters",
                "new",
                "old=" + MASTER_KEY,
                null
        );
        EncryptionService rotatedService = new EncryptionService(mock(StringEncryptor.class), rotated);

        assertThat(rotatedService.decrypt(encrypted)).isEqualTo("rotatable value");
    }

    @Test
    void decryptsWhenSameKeyIdWasWrittenWithDifferentMaterial() {
        // Ciphertext stamped primary with OLD material; ring has primary=NEW and legacy=OLD.
        EncryptionService oldPrimary = service("primary", "");
        String encrypted = oldPrimary.encrypt("mixed rotation value");

        KeyProvider mixed = new ConfigKeyProvider(
                testEnv(),
                "new-master-key-with-at-least-thirty-two-characters",
                "primary",
                "legacy=" + MASTER_KEY,
                null
        );
        EncryptionService mixedService = new EncryptionService(mock(StringEncryptor.class), mixed);

        assertThat(encrypted).startsWith("TFENC:v2:primary:");
        assertThat(mixedService.decrypt(encrypted)).isEqualTo("mixed rotation value");
    }

    @Test
    void delegatesLegacyCiphertextToJasyptDecryptor() {
        StringEncryptor legacy = mock(StringEncryptor.class);
        when(legacy.decrypt("legacy-ciphertext")).thenReturn("legacy plaintext");
        EncryptionService service = new EncryptionService(legacy, keyProvider("primary", ""));

        assertThat(service.decrypt("legacy-ciphertext")).isEqualTo("legacy plaintext");
    }

    @Test
    void rejectsMissingMasterKey() {
        assertThatThrownBy(() -> new ConfigKeyProvider(testEnv(), "", "primary", "", null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("master key");
    }

    private EncryptionService service(String keyId, String previousKeys) {
        return new EncryptionService(mock(StringEncryptor.class), keyProvider(keyId, previousKeys));
    }

    private static KeyProvider keyProvider(String keyId, String previousKeys) {
        return new ConfigKeyProvider(testEnv(), MASTER_KEY, keyId, previousKeys, null);
    }

    private static Environment testEnv() {
        return new MockEnvironment();
    }
}
