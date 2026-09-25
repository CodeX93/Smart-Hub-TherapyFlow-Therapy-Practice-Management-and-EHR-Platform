package com.smart.therapy.flow.common.converter;

import com.smart.therapy.flow.common.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * JPA Converter for transparently encrypting/decrypting sensitive string data.
 * Used for PHI (Protected Health Information) fields in entities.
 */
@Converter
@Component
public class EncryptedStringConverter implements AttributeConverter<String, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EncryptedStringConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedStringConverter");
        }
        if (encryptionService.isEncrypted(attribute)) {
            return attribute;
        }
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedStringConverter");
        }
        return encryptionService.decrypt(dbData);
    }
}
