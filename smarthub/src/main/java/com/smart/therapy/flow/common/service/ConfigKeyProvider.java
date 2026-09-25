package com.smart.therapy.flow.common.service;

import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dev/test {@link KeyProvider}: derives DEK and search-HMAC keys from config secrets.
 * Refuses to start when an active Spring profile is {@code prod} / {@code production}.
 */
public class ConfigKeyProvider implements KeyProvider {

    static final String DEK_DERIVATION_INFO = "therapyflow:phi:aes-gcm:v2";
    static final String SEARCH_HMAC_DERIVATION_INFO = "therapyflow:phi:search-hmac:v1";

    private final String currentKeyId;
    private final Map<String, SecretKey> dekRing;
    private final byte[] searchHmacKey;

    public ConfigKeyProvider(
            Environment environment,
            String masterKey,
            String keyId,
            String previousKeys,
            String searchHmacKeyMaterial
    ) {
        assertNotProduction(environment);
        if (!StringUtils.hasText(masterKey)) {
            throw new IllegalStateException("PHI encryption master key is required");
        }
        if (!StringUtils.hasText(keyId) || keyId.contains(":") || keyId.contains(",")) {
            throw new IllegalStateException("PHI encryption key id is invalid");
        }
        this.currentKeyId = keyId.trim();
        this.dekRing = buildDekRing(this.currentKeyId, masterKey.trim(), previousKeys);
        this.searchHmacKey = deriveSearchHmacKey(masterKey.trim(), searchHmacKeyMaterial);
        assertDistinctKeyMaterial();
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

    private void assertDistinctKeyMaterial() {
        byte[] dekBytes = dekRing.get(currentKeyId).getEncoded();
        if (MessageDigest.isEqual(dekBytes, searchHmacKey)) {
            throw new IllegalStateException("Search HMAC key must not equal DEK material");
        }
    }

    private static void assertNotProduction(Environment environment) {
        if (environment == null) {
            return;
        }
        for (String profile : environment.getActiveProfiles()) {
            if ("prod".equalsIgnoreCase(profile) || "production".equalsIgnoreCase(profile)) {
                throw new IllegalStateException(
                        "ConfigKeyProvider cannot be used with the prod profile; set APP_ENCRYPTION_PROVIDER=keyvault");
            }
        }
    }

    private static Map<String, SecretKey> buildDekRing(String currentKeyId, String masterKey, String previousKeys) {
        Map<String, SecretKey> keys = new LinkedHashMap<>();
        keys.put(currentKeyId, deriveAesKey(masterKey));
        if (!StringUtils.hasText(previousKeys)) {
            return Map.copyOf(keys);
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
            String value = entry.substring(separator + 1).trim();
            if (!StringUtils.hasText(id) || id.contains(":") || keys.containsKey(id)) {
                throw new IllegalStateException("Invalid or duplicate previous PHI encryption key id");
            }
            keys.put(id, deriveAesKey(value));
        }
        return Map.copyOf(keys);
    }

    private static SecretKey deriveAesKey(String masterKey) {
        return new SecretKeySpec(sha256(DEK_DERIVATION_INFO, masterKey), "AES");
    }

    private static byte[] deriveSearchHmacKey(String masterKey, String searchHmacKeyMaterial) {
        String material = StringUtils.hasText(searchHmacKeyMaterial) ? searchHmacKeyMaterial.trim() : masterKey;
        return sha256(SEARCH_HMAC_DERIVATION_INFO, material);
    }

    private static byte[] sha256(String info, String material) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(info.getBytes(StandardCharsets.UTF_8));
            digest.update(material.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to initialize PHI key material", e);
        }
    }
}
