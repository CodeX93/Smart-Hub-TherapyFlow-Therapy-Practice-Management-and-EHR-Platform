package com.smart.therapy.flow.common.service;

import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.client.entity.ClientContact;
import com.smart.therapy.flow.client.entity.ClientNameBlindIndex;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.common.tenant.TenantContext;
import com.smart.therapy.flow.common.util.PhoneNormalizationUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.List;
import java.util.Locale;

/**
 * HMAC-SHA256 blind indexes for Approach C exact-match search.
 * Digests are never logged. Search key material must differ from the DEK.
 */
@Slf4j
@Service
public class BlindIndexService {

    public enum Kind {
        CLIENT_ID,
        FULL_NAME,
        /** Whitespace-separated tokens of a client full name (first/last name search). */
        NAME_TOKEN,
        CONTACT,
        DATE_OF_BIRTH
    }

    public enum SearchMode {
        LEGACY,
        DUAL,
        BLIND_ONLY;

        public static SearchMode from(String raw) {
            if (!StringUtils.hasText(raw)) {
                return LEGACY;
            }
            return SearchMode.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        }

        public boolean writesBlindIndexes() {
            return this == DUAL || this == BLIND_ONLY;
        }
    }

    private final KeyProvider keyProvider;
    private final SearchMode searchMode;
    private final String tenantPepper;

    public BlindIndexService(
            KeyProvider keyProvider,
            @Value("${app.phi-encryption.search.mode:legacy}") String searchMode,
            @Value("${app.encryption.search-hmac-tenant-pepper:}") String tenantPepper
    ) {
        this.keyProvider = keyProvider;
        this.searchMode = SearchMode.from(searchMode);
        this.tenantPepper = tenantPepper == null ? "" : tenantPepper;
        assertHmacDistinctFromDek();
    }

    public SearchMode getSearchMode() {
        return searchMode;
    }

    /**
     * Exposes the key provider for HMAC key version detection.
     * Package-private to limit access to internal services only.
     */
    KeyProvider getKeyProvider() {
        return keyProvider;
    }

    /**
     * Dual-write helper: compute and set blind-index fields on client and contacts.
     * No-op when {@code app.phi-encryption.search.mode=legacy}.
     * Also rebuilds {@link Client#getNameBlindIndexes()} token digests for first/last-name search.
     * Caller must persist the mutated entities.
     */
    public void updateBlindIndexes(Client client, List<ClientContact> contacts) {
        if (!searchMode.writesBlindIndexes()) {
            return;
        }
        if (client != null) {
            updateClientScalarBlindIndexes(client);
            syncNameTokenBlindIndexes(client);
        }
        updateContactBlindIndexes(contacts);
    }

    /**
     * Update the blind indexes stored directly on the client row without touching
     * its cascaded name-token collection. Client creation uses this before the
     * first flush, then creates the token collection once the client row exists.
     */
    public void updateClientScalarBlindIndexes(Client client) {
        if (!searchMode.writesBlindIndexes() || client == null) {
            return;
        }
        if (StringUtils.hasText(client.getClientId())) {
            client.setClientIdBlindIdx(compute(Kind.CLIENT_ID, normalizeMrn(client.getClientId())));
        }
        if (StringUtils.hasText(client.getFullName())) {
            client.setFullNameBlindIdx(compute(Kind.FULL_NAME, normalizeFullName(client.getFullName())));
        }
        if (client.getDateOfBirth() != null) {
            client.setDateOfBirthBlindIdx(
                    compute(Kind.DATE_OF_BIRTH, normalizeDateOfBirth(client.getDateOfBirth())));
        } else {
            client.setDateOfBirthBlindIdx(null);
        }
    }

    /**
     * Update only contact blind indexes. This is intentionally separate from
     * {@link #updateBlindIndexes(Client, List)} so callers that reload contacts
     * after persisting a client do not rebuild the client's orphan-removal name
     * token collection a second time in the same transaction.
     */
    public void updateContactBlindIndexes(List<ClientContact> contacts) {
        if (!searchMode.writesBlindIndexes() || contacts == null) {
            return;
        }
        for (ClientContact contact : contacts) {
            applyContactBlindIndex(contact);
        }
    }

    /**
     * Complete words first, then distinct Unicode-safe prefixes of at least two characters.
     * Callers persist only the resulting HMAC digests.
     */
    public List<String> nameSearchTerms(String fullName) {
        if (!StringUtils.hasText(fullName)) return List.of();
        var words = normalizeFullName(fullName).split(" ");
        var terms = new java.util.LinkedHashSet<String>();
        for (String word : words) if (!word.isBlank()) terms.add(word);
        for (String word : words) {
            int length = word.codePointCount(0, word.length());
            for (int size = 2; size < length; size++) {
                terms.add(word.substring(0, word.offsetByCodePoints(0, size)));
            }
        }
        return List.copyOf(terms);
    }

    /** Replace digests in the initialized cascade/orphan-removal collection. */
    public void syncNameTokenBlindIndexes(Client client) {
        if (!searchMode.writesBlindIndexes() || client == null) {
            return;
        }
        List<ClientNameBlindIndex> tokens = client.getNameBlindIndexes();
        if (tokens == null) {
            tokens = new java.util.ArrayList<>();
            client.setNameBlindIndexes(tokens);
        }
        if (!StringUtils.hasText(client.getFullName())) {
            tokens.clear();
            return;
        }
        // Reuse rows by ordinal: inserting replacements before orphan deletes violates
        // the unique (client_id, token_ord) constraint during Hibernate flush.
        var existingByOrdinal = new java.util.HashMap<Integer, ClientNameBlindIndex>();
        for (ClientNameBlindIndex token : tokens) {
            existingByOrdinal.put(token.getTokenOrd(), token);
        }
        List<String> parts = nameSearchTerms(client.getFullName());
        int ord = 0;
        for (String part : parts) {
            if (!StringUtils.hasText(part)) {
                continue;
            }
            ClientNameBlindIndex token = existingByOrdinal.get(ord);
            if (token == null) {
                token = ClientNameBlindIndex.builder().client(client).tokenOrd(ord).build();
                tokens.add(token);
            }
            token.setTokenBlindIdx(compute(Kind.NAME_TOKEN, part));
            ord++;
        }
        int tokenCount = ord;
        tokens.removeIf(token -> token.getTokenOrd() >= tokenCount);
    }

    public void applyContactBlindIndex(ClientContact contact) {
        if (!searchMode.writesBlindIndexes() || contact == null) {
            return;
        }
        if (!StringUtils.hasText(contact.getContactValue())) {
            contact.setContactBlindIdx(null);
            return;
        }
        String normalized = normalizeContactValue(contact.getContactType(), contact.getContactValue());
        contact.setContactBlindIdx(compute(Kind.CONTACT, normalized));
    }

    public byte[] compute(Kind kind, String normalizedValue) {
        if (kind == null) {
            throw new IllegalArgumentException("Blind index kind is required");
        }
        if (!StringUtils.hasText(normalizedValue)) {
            throw new IllegalArgumentException("Normalized value is required for blind index");
        }
        try {
            byte[] hmacKey = keyProvider.getSearchHmacKey();
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacKey, "HmacSHA256"));
            mac.update(tenantSalt());
            mac.update((byte) 0);
            mac.update(kind.name().getBytes(StandardCharsets.UTF_8));
            mac.update((byte) 0);
            mac.update(normalizedValue.getBytes(StandardCharsets.UTF_8));
            return mac.doFinal();
        } catch (GeneralSecurityException e) {
            log.error("Blind index computation failed: errorType={}", e.getClass().getSimpleName());
            throw new IllegalStateException("Blind index computation failed", e);
        }
    }

    public String normalizeMrn(String raw) {
        return raw == null ? null : raw.trim().toUpperCase(Locale.ROOT);
    }

    public String normalizeEmail(String raw) {
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizeFullName(String raw) {
        if (raw == null) {
            return null;
        }
        return raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    public String normalizeDateOfBirth(java.time.LocalDate dob) {
        return dob == null ? null : dob.toString();
    }

    public String normalizePhone(String raw) {
        String e164 = PhoneNormalizationUtil.normalizePhoneE164(raw);
        return e164 != null ? e164 : (raw == null ? null : raw.trim());
    }

    public String normalizeContactValue(ContactType type, String raw) {
        if (type != null && type.isEmail()) {
            return normalizeEmail(raw);
        }
        if (type != null && type.isPhone()) {
            return normalizePhone(raw);
        }
        return raw == null ? null : raw.trim().toLowerCase(Locale.ROOT);
    }

    private byte[] tenantSalt() {
        Long orgId = TenantContext.getOrganisationId();
        String orgPart = orgId != null ? Long.toString(orgId) : "unknown-org";
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update("therapyflow:phi:tenant-salt:v1".getBytes(StandardCharsets.UTF_8));
            digest.update(orgPart.getBytes(StandardCharsets.UTF_8));
            digest.update((byte) 0);
            digest.update(tenantPepper.getBytes(StandardCharsets.UTF_8));
            return digest.digest();
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Unable to derive tenant salt", e);
        }
    }

    private void assertHmacDistinctFromDek() {
        try {
            byte[] hmac = keyProvider.getSearchHmacKey();
            SecretKey dek = keyProvider.getDataEncryptionKey(keyProvider.getCurrentKeyId());
            if (dek != null && dek.getEncoded() != null && MessageDigest.isEqual(hmac, dek.getEncoded())) {
                throw new IllegalStateException("Search HMAC key must not equal DEK material");
            }
        } catch (IllegalStateException e) {
            throw e;
        } catch (RuntimeException e) {
            log.warn("Could not assert HMAC≠DEK at BlindIndexService startup: {}", e.getClass().getSimpleName());
        }
    }
}
