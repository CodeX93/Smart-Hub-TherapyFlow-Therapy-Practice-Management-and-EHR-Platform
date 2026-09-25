package com.smart.therapy.flow.common.converter;

import com.smart.therapy.flow.common.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Deterministic AES-GCM converter for searchable PHI columns (exact-match queries).
 *
 * @deprecated Approach C uses {@link EncryptedStringConverter} + HMAC blind indexes.
 * Kept only so remaining {@code TFENC:v2d:} rows can be read during re-encrypt backfill.
 * Do not annotate new entity fields with this converter.
 */
@Deprecated(since = "Approach C Wave 2", forRemoval = true)
@Converter
@Component
public class EncryptedSearchableStringConverter implements AttributeConverter<String, String> {

    public static final String PURPOSE_CLIENT_FULL_NAME = "clients.full_name";
    public static final String PURPOSE_CONTACT_VALUE = "client_contacts.contact_value";

    private static EncryptionService encryptionService;
    private final String purpose;

    public EncryptedSearchableStringConverter() {
        this(PURPOSE_CLIENT_FULL_NAME);
    }

    protected EncryptedSearchableStringConverter(String purpose) {
        this.purpose = purpose;
    }

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EncryptedSearchableStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedSearchableStringConverter");
        }
        if (encryptionService.isEncrypted(attribute)) {
            return attribute;
        }
        return encryptionService.encryptDeterministic(attribute, purpose);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedSearchableStringConverter");
        }
        return encryptionService.decrypt(dbData);
    }

    @Converter
    @Component
    @Deprecated(since = "Approach C Wave 2", forRemoval = true)
    public static class ContactValue extends EncryptedSearchableStringConverter {
        public ContactValue() {
            super(PURPOSE_CONTACT_VALUE);
        }
    }
}
