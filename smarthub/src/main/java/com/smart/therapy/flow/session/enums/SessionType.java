package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Session type enumeration
 * Represents how the therapy session is conducted
 */
public enum SessionType {
    ONLINE("online", "Online Session", "Virtual therapy session via video conferencing"),
    IN_PERSON("in-person", "In-Person Session", "Face-to-face therapy session at physical location");

    private final String value;
    private final String displayName;
    private final String description;

    SessionType(String value, String displayName, String description) {
        this.value = value;
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static SessionType fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(SessionType.values())
                .filter(type -> type.value.equalsIgnoreCase(value) || 
                               type.name().equalsIgnoreCase(value) ||
                               type.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid session type: " + value));
    }

    public boolean isOnline() {
        return this == ONLINE;
    }

    public boolean isInPerson() {
        return this == IN_PERSON;
    }
}
