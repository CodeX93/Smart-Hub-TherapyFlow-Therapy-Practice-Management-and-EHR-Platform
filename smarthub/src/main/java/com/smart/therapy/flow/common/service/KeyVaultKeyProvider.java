package com.smart.therapy.flow.common.service;

import com.azure.identity.DefaultAzureCredentialBuilder;
import com.azure.security.keyvault.secrets.SecretClient;
import com.azure.security.keyvault.secrets.SecretClientBuilder;
import com.azure.security.keyvault.secrets.models.KeyVaultSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Azure Key Vault–backed {@link KeyProvider}.
 * Loads DEK material ({@code APP_ENCRYPTION_KEK_NAME}) and search-HMAC material
 * ({@code APP_SEARCH_HMAC_KEY_NAME}) from {@code AZURE_KEYVAULT_URI}.
 * Optional previous DEKs via {@code app.encryption.previous-keys} as
 * {@code keyId=secretName,keyId2=secretName2} for rotation decrypt.
 * <p>
 * Current DEK material is always SHA-256-derived (same as {@link ConfigKeyProvider}).
 * If the secret is also a 32-byte base64 blob, a {@code <keyId>-raw} alternate is added
 * so ciphertext written under the older raw-AES Key Vault path still decrypts.
 */
@Slf4j
public class KeyVaultKeyProvider implements KeyProvider {

    private final String currentKeyId;
    private final Map<String, SecretKey> dekRing;
    private final byte[] searchHmacKey;

    public KeyVaultKeyProvider(
            String vaultUri,
            String kekSecretName,
            String searchHmacSecretName,
            String keyId,
            String previousKeys
    ) {
        if (!StringUtils.hasText(vaultUri)) {
            throw new IllegalStateException(
                    "AZURE_KEYVAULT_URI is required when APP_ENCRYPTION_PROVIDER=keyvault");
        }
        if (!StringUtils.hasText(kekSecretName)) {
            throw new IllegalStateException(
                    "APP_ENCRYPTION_KEK_NAME is required when APP_ENCRYPTION_PROVIDER=keyvault");
        }
        if (!StringUtils.hasText(searchHmacSecretName)) {
            throw new IllegalStateException(
                    "APP_SEARCH_HMAC_KEY_NAME is required when APP_ENCRYPTION_PROVIDER=keyvault");
        }
        if (!StringUtils.hasText(keyId) || keyId.contains(":") || keyId.contains(",")) {
            throw new IllegalStateException("PHI encryption key id is invalid");
        }

        this.currentKeyId = keyId.trim();
        try {
            SecretClient secretClient = new SecretClientBuilder()
                    .vaultUrl(vaultUri.trim())
                    .credential(new DefaultAzureCredentialBuilder().build())
                    .buildClient();

            String dekMaterial = requireSecret(secretClient, kekSecretName.trim());
            String hmacMaterial = requireSecret(secretClient, searchHmacSecretName.trim());
            // Current encrypt key always matches ConfigKeyProvider derivation so existing
            // encryption-password / master-key ciphertext keeps working after Key Vault cutover.
            SecretKey dek = deriveAesKey(dekMaterial, ConfigKeyProvider.DEK_DERIVATION_INFO);
            this.searchHmacKey = toKeyBytes(hmacMaterial, ConfigKeyProvider.SEARCH_HMAC_DERIVATION_INFO);
            if (MessageDigest.isEqual(dek.getEncoded(), this.searchHmacKey)) {
                throw new IllegalStateException("Search HMAC key must not equal DEK material");
            }

            Map<String, SecretKey> keys = new LinkedHashMap<>();
            keys.put(this.currentKeyId, dek);
            // openssl rand -base64 32 was previously used as raw AES; keep it for decrypt only.
            addRawAlternateIfPresent(keys, this.currentKeyId + "-raw", dekMaterial);
            loadPreviousKeys(secretClient, previousKeys, keys);
            this.dekRing = Map.copyOf(keys);
            log.info("KeyVaultKeyProvider initialized: vault configured, keyId={}, ringSize={}",
                    this.currentKeyId, keys.size());
        } catch (IllegalStateException e) {
            throw e;
        } catch (NoClassDefFoundError e) {
            throw new UnsupportedOperationException(
                    "Azure Key Vault SDK / identity libraries are not available on the classpath. "
                            + "Add azure-security-keyvault-secrets and azure-identity, and configure "
                            + "AZURE_KEYVAULT_URI, APP_ENCRYPTION_KEK_NAME, APP_SEARCH_HMAC_KEY_NAME.",
                    e);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to load PHI keys from Azure Key Vault. Verify managed identity access "
                            + "and secrets " + kekSecretName + " / " + searchHmacSecretName,
                    e);
        }
    }

    /** Backward-compatible ctor without previous-key ring. */
    public KeyVaultKeyProvider(
            String vaultUri,
            String kekSecretName,
            String searchHmacSecretName,
            String keyId
    ) {
        this(vaultUri, kekSecretName, searchHmacSecretName, keyId, null);
    }

    private static void loadPreviousKeys(
            SecretClient secretClient, String previousKeys, Map<String, SecretKey> keys) {
        if (!StringUtils.hasText(previousKeys)) {
            return;
        }
        for (String entry : previousKeys.split(",")) {
            if (!StringUtils.hasText(entry)) {
                continue;
            }
            int separator = entry.indexOf('=');
            if (separator <= 0 || separator == entry.length() - 1) {
                throw new IllegalStateException("Invalid previous PHI encryption key configuration");
            }
            String id = entry.substring(0, separator).trim();
            String secretName = entry.substring(separator + 1).trim();
            if (!StringUtils.hasText(id) || id.contains(":") || keys.containsKey(id)) {
                throw new IllegalStateException("Invalid or duplicate previous PHI encryption key id");
            }
            String material = requireSecret(secretClient, secretName);
            keys.put(id, deriveAesKey(material, ConfigKeyProvider.DEK_DERIVATION_INFO));
            addRawAlternateIfPresent(keys, id + "-raw", material);
        }
    }

    private static void addRawAlternateIfPresent(Map<String, SecretKey> keys, String altId, String material) {
        if (keys.containsKey(altId)) {
            return;
        }
        byte[] raw = tryDecodeRawAes256(material);
        if (raw == null) {
            return;
        }
        for (SecretKey existing : keys.values()) {
            if (MessageDigest.isEqual(existing.getEncoded(), raw)) {
                return;
            }
        }
        keys.put(altId, new SecretKeySpec(raw, "AES"));
    }

    private static SecretKey deriveAesKey(String material, String info) {
        return new SecretKeySpec(sha256(info, material), "AES");
    }

    @Override
    public SecretKey getDataEncryptionKey(String keyId) {
        SecretKey key = dekRing.get(keyId);
        if (key == null) {
            throw new IllegalStateException("Encryption key is unavailable");
        }
        return key;
    }

    @Override
    public byte[] getSearchHmacKey() {
        return searchHmacKey.clone();
    }

    @Override
    public String getCurrentKeyId() {
        return currentKeyId;
    }

    @Override
    public List<String> getAvailableKeyIds() {
        return List.copyOf(dekRing.keySet());
    }

    private static String requireSecret(SecretClient client, String name) {
        KeyVaultSecret secret = client.getSecret(name);
        if (secret == null || !StringUtils.hasText(secret.getValue())) {
            throw new IllegalStateException("Key Vault secret is empty: " + name);
        }
        return secret.getValue().trim();
    }

    /** Prefer raw 32-byte base64 when present; otherwise SHA-256 derive (HMAC path). */
    private static byte[] toKeyBytes(String material, String info) {
        byte[] raw = tryDecodeRawAes256(material);
        if (raw != null) {
            return raw;
        }
        return sha256(info, material);
    }

    private static byte[] tryDecodeRawAes256(String material) {
        try {
            byte[] decoded = Base64.getDecoder().decode(material);
            if (decoded.length == 32) {
                return decoded;
            }
        } catch (IllegalArgumentException ignored) {
            // not base64
        }
        return null;
    }

    private static byte[] sha256(String info, String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(info.getBytes(StandardCharsets.UTF_8));
            digest.update(material.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to derive key material from Key Vault secret", e);
        }
    }
}
