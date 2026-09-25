package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

public enum RecurrenceEndMode {
    COUNT("count"),
    UNTIL("until");

    private final String value;

    RecurrenceEndMode(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static RecurrenceEndMode fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(mode -> mode.value.equals(normalized) || mode.name().toLowerCase(Locale.ROOT).equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid recurrence end mode: " + value));
    }
}
