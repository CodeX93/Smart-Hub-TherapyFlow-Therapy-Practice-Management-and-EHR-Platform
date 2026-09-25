package com.smart.therapy.flow.common.service;

import lombok.extern.slf4j.Slf4j;
import org.jasypt.encryption.StringEncryptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Service for encrypting and decrypting sensitive data.
 *
 * New values use versioned AES-256-GCM authenticated encryption ({@code TFENC:v2:}).
 * Deterministic {@code TFENC:v2d:} encryption is deprecated — Approach C uses blind indexes.
 * The Jasypt encryptor is retained only to read ciphertext written before the v2 format.
 * Key material is supplied by {@link KeyProvider}.
 */
@Slf4j
@Service
public class EncryptionService {

    private static final String FORMAT_PREFIX = "TFENC:v2:";
    private static final String FORMAT_PREFIX_DET = "TFENC:v2d:";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;

    /**
     * @deprecated Approach C no longer writes deterministic ciphertext for these columns.
     */
    @Deprecated(since = "Approach C Wave 2", forRemoval = true)
    public static final Set<String> DETERMINISTIC_COLUMNS = Set.of();

    private final StringEncryptor stringEncryptor;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String currentKeyId;
    private final Map<String, SecretKey> keyRing;
    private final boolean allowDeterministicEncrypt;

    @Autowired
    public EncryptionService(
            StringEncryptor stringEncryptor,
            KeyProvider keyProvider,
            @org.springframework.beans.factory.annotation.Value("${app.phi-encryption.search.mode:legacy}")
            String searchMode
    ) {
        this.stringEncryptor = stringEncryptor;
        if (keyProvider == null) {
            throw new IllegalStateException("PHI KeyProvider is required");
        }
        this.currentKeyId = keyProvider.getCurrentKeyId();
        this.keyRing = buildKeyRing(keyProvider);
        this.allowDeterministicEncrypt = !"blind_only".equalsIgnoreCase(
                searchMode == null ? "" : searchMode.trim());
    }

    /** Test helper — defaults to dual (allows deterministic encrypt for dual-mode tests). */
    public EncryptionService(StringEncryptor stringEncryptor, KeyProvider keyProvider) {
        this(stringEncryptor, keyProvider, "dual");
    }

    public String encrypt(String plaintext) {
        return encryptInternal(plaintext, null);
    }

    /**
     * Deterministic encrypt for legacy dual-mode lookups of unreencrypted {@code TFENC:v2d:} rows.
     *
     * @deprecated Prefer blind-index equality. Throws when {@code search.mode=blind_only}.
     */
    @Deprecated(since = "Approach C Wave 2", forRemoval = true)
    public String encryptDeterministic(String plaintext, String purpose) {
        if (!allowDeterministicEncrypt) {
            throw new IllegalStateException(
                    "Deterministic PHI encryption is retired when app.phi-encryption.search.mode=blind_only");
        }
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        if (purpose == null || purpose.isBlank()) {
            throw new IllegalArgumentException("Deterministic encryption purpose is required");
        }
        return encryptInternal(plaintext, purpose.trim());
    }

    private String encryptInternal(String plaintext, String purpose) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }
        try {
            byte[] iv;
            String prefix;
            byte[] aadBytes;
            if (purpose == null) {
                iv = new byte[GCM_IV_BYTES];
                secureRandom.nextBytes(iv);
                prefix = FORMAT_PREFIX;
                aadBytes = aad(currentKeyId);
            } else {
                iv = deriveDeterministicIv(purpose, plaintext);
                prefix = FORMAT_PREFIX_DET;
                aadBytes = aadDeterministic(currentKeyId);
            }

            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, keyRing.get(currentKeyId), new GCMParameterSpec(GCM_TAG_BITS, iv));
            cipher.updateAAD(aadBytes);
            byte[] encrypted = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            byte[] payload = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, payload, 0, iv.length);
            System.arraycopy(encrypted, 0, payload, iv.length, encrypted.length);
            return prefix + currentKeyId + ":" + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException e) {
            log.error("PHI encryption failed: errorType={}", e.getClass().getSimpleName());
            throw new IllegalStateException("Encryption failed", e);
        }
    }

    public String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isEmpty()) {
            return encrypted;
        }
        if (encrypted.startsWith(FORMAT_PREFIX_DET)) {
            return decryptVersioned(encrypted, FORMAT_PREFIX_DET, true);
        }
        if (encrypted.startsWith(FORMAT_PREFIX)) {
            return decryptVersioned(encrypted, FORMAT_PREFIX, false);
        }
        // Legacy Jasypt / non-versioned ciphertext, or plaintext passthrough during migration.
        try {
            return decryptLegacy(encrypted);
        } catch (RuntimeException ex) {
            return encrypted;
        }
    }

    private String decryptVersioned(String encrypted, String prefix, boolean deterministic) {
        try {
            String remainder = encrypted.substring(prefix.length());
            String[] parts = remainder.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed encrypted value");
            }
            String keyId = parts[0];
            String encodedPayload = parts[1];
            byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
            String canonicalPayload = Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
            if (!MessageDigest.isEqual(
                    encodedPayload.getBytes(StandardCharsets.US_ASCII),
                    canonicalPayload.getBytes(StandardCharsets.US_ASCII))) {
                throw new IllegalArgumentException("Malformed encrypted value");
            }
            if (payload.length <= GCM_IV_BYTES) {
                throw new IllegalArgumentException("Malformed encrypted value");
            }
            byte[] iv = new byte[GCM_IV_BYTES];
            byte[] ciphertext = new byte[payload.length - GCM_IV_BYTES];
            System.arraycopy(payload, 0, iv, 0, iv.length);
            System.arraycopy(payload, iv.length, ciphertext, 0, ciphertext.length);

            // Prefer the key id stamped in ciphertext, then try the rest of the ring.
            // Same keyId can have been written with different material (e.g. accidental DEK
            // rotation); AAD stays bound to the stamped keyId.
            LinkedHashSet<SecretKey> candidates = new LinkedHashSet<>();
            SecretKey primary = keyRing.get(keyId);
            if (primary != null) {
                candidates.add(primary);
            }
            candidates.addAll(keyRing.values());
            if (candidates.isEmpty()) {
                throw new IllegalStateException("Encryption key is unavailable");
            }

            for (SecretKey key : candidates) {
                try {
                    Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
                    cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_TAG_BITS, iv));
                    cipher.updateAAD(deterministic ? aadDeterministic(keyId) : aad(keyId));
                    return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
                } catch (AEADBadTagException e) {
                    // try next key in the ring
                }
            }
            // Soft-fail like legacy decrypt: return ciphertext unchanged so JPA reads do not 500
            // entire list/detail endpoints when ciphertext was written under an unavailable key
            // (e.g. ClientHub migration master key vs production Key Vault DEK).
            // convertToDatabaseColumn short-circuits on isEncrypted(), so write-back stays safe.
            log.error("PHI ciphertext authentication failed for all {} key(s) in ring; returning undecrypted value",
                    candidates.size());
            return encrypted;
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            log.error("PHI decryption failed: errorType={}; returning undecrypted value",
                    e.getClass().getSimpleName());
            return encrypted;
        }
    }

    public boolean isEncrypted(String value) {
        if (value == null || value.isEmpty()) {
            return false;
        }
        return value.startsWith(FORMAT_PREFIX)
                || value.startsWith(FORMAT_PREFIX_DET)
                || (value.startsWith("ENC(") && value.endsWith(")"));
    }

    private byte[] deriveDeterministicIv(String purpose, String plaintext) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(keyRing.get(currentKeyId).getEncoded(), "HmacSHA256"));
        mac.update(purpose.getBytes(StandardCharsets.UTF_8));
        mac.update((byte) 0);
        mac.update(plaintext.trim().getBytes(StandardCharsets.UTF_8));
        byte[] digest = mac.doFinal();
        byte[] iv = new byte[GCM_IV_BYTES];
        System.arraycopy(digest, 0, iv, 0, GCM_IV_BYTES);
        return iv;
    }

    private String decryptLegacy(String encrypted) {
        try {
            return stringEncryptor.decrypt(encrypted);
        } catch (Exception e) {
            log.error("Legacy PHI decryption failed: errorType={}", e.getClass().getSimpleName());
            throw new IllegalStateException("Decryption failed", e);
        }
    }

    private static Map<String, SecretKey> buildKeyRing(KeyProvider keyProvider) {
        Map<String, SecretKey> keys = new HashMap<>();
        for (String id : keyProvider.getAvailableKeyIds()) {
            keys.put(id, keyProvider.getDataEncryptionKey(id));
        }
        if (keys.isEmpty() || !keys.containsKey(keyProvider.getCurrentKeyId())) {
            throw new IllegalStateException("PHI encryption key ring is empty or missing current key");
        }
        return Map.copyOf(keys);
    }

    private static byte[] aad(String keyId) {
        return ("therapyflow:phi:v2:" + keyId).getBytes(StandardCharsets.UTF_8);
    }

    private static byte[] aadDeterministic(String keyId) {
        return ("therapyflow:phi:v2d:" + keyId).getBytes(StandardCharsets.UTF_8);
    }
}
