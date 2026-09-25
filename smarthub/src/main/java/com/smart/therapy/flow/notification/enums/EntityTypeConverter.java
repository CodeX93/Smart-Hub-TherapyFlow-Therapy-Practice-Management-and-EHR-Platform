package com.smart.therapy.flow.notification.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Locale;

@Converter(autoApply = false)
public class EntityTypeConverter implements AttributeConverter<EntityType, String> {

    @Override
    public String convertToDatabaseColumn(EntityType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase(Locale.ROOT);
    }

    @Override
    public EntityType convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        String normalized = dbData.trim().toUpperCase(Locale.ROOT);
        try {
            return EntityType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return EntityType.GENERAL;
        }
    }
}
