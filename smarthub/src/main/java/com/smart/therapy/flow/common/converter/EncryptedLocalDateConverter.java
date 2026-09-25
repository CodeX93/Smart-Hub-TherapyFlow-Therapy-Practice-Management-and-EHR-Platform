package com.smart.therapy.flow.common.converter;

import com.smart.therapy.flow.common.service.EncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * Encrypts {@link LocalDate} PHI (e.g. DOB) as ISO-8601 text under {@code TFENC:v2}.
 * Entity stays {@link LocalDate}; DB column must be TEXT (see tenant V58).
 */
@Converter
@Component
public class EncryptedLocalDateConverter implements AttributeConverter<LocalDate, String> {

    private static EncryptionService encryptionService;

    @Autowired
    public void setEncryptionService(EncryptionService service) {
        EncryptedLocalDateConverter.encryptionService = service;
    }

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        if (attribute == null) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedLocalDateConverter");
        }
        return encryptionService.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        if (encryptionService == null) {
            throw new IllegalStateException("EncryptionService not initialized in EncryptedLocalDateConverter");
        }
        String plain = encryptionService.isEncrypted(dbData)
                ? encryptionService.decrypt(dbData)
                : dbData;
        try {
            return LocalDate.parse(plain.trim());
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("Unable to parse encrypted/plain date of birth value", ex);
        }
    }
}
