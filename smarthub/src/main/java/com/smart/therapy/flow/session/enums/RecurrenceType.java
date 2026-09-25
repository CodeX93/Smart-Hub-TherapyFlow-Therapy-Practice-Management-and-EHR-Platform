package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum RecurrenceType {
    WEEKLY("weekly"),
    MONTHLY("monthly");

    private final String value;

    RecurrenceType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RecurrenceType fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(type -> type.value.equals(normalized) || type.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid recurrence type: " + value));
    }
}
