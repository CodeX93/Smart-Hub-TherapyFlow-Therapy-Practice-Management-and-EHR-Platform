package com.smart.therapy.flow.common.service;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Abstraction over PHI data-encryption keys (DEK) and the search-HMAC key.
 * Production must use Azure Key Vault; config-backed keys are for local/test only.
 */
public interface KeyProvider {

    /**
     * AES-256 DEK for the given key id (used by {@code TFENC:v2} / {@code TFENC:v2d}).
     */
    SecretKey getDataEncryptionKey(String keyId);

    /**
     * Raw key material for HMAC-SHA256 blind indexes. Must never equal DEK material.
     */
    byte[] getSearchHmacKey();

    String getCurrentKeyId();

    /** All DEK ids available for decrypt (current + previous). */
    List<String> getAvailableKeyIds();
}
