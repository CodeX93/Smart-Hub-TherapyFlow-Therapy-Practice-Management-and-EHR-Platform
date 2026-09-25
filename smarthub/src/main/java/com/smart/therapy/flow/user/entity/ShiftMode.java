package com.smart.therapy.flow.user.entity;

import java.util.Locale;

/**
 * Per-shift session modality for therapist working hours.
 */
public enum ShiftMode {
    VIRTUAL,
    IN_PERSON,
    BOTH;

    public static ShiftMode fromValue(String value) {
        if (value == null || value.isBlank()) {
            return BOTH;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT).replace('_', '-');
        return switch (normalized) {
            case "virtual" -> VIRTUAL;
            case "in-person", "inperson" -> IN_PERSON;
            case "both" -> BOTH;
            default -> throw new IllegalArgumentException("Invalid shift mode: " + value);
        };
    }

    public String toJsonValue() {
        return switch (this) {
            case VIRTUAL -> "virtual";
            case IN_PERSON -> "in-person";
            case BOTH -> "both";
        };
    }
}

