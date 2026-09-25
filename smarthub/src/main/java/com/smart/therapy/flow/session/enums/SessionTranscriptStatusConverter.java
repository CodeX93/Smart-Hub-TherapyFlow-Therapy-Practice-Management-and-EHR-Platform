package com.smart.therapy.flow.session.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class SessionTranscriptStatusConverter implements AttributeConverter<SessionTranscriptStatus, String> {

    @Override
    public String convertToDatabaseColumn(SessionTranscriptStatus status) {
        if (status == null) {
            return null;
        }
        return status.getValue();
    }

    @Override
    public SessionTranscriptStatus convertToEntityAttribute(String dbValue) {
        if (dbValue == null || dbValue.isBlank()) {
            return null;
        }
        return SessionTranscriptStatus.fromValue(dbValue);
    }
}
