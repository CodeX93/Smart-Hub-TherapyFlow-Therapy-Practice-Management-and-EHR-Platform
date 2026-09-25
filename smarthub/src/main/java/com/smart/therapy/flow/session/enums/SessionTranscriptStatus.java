package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Map;

public enum SessionTranscriptStatus {
    RECORDING("recording"),
    PROCESSING("processing"),
    READY("ready"),
    FAILED("failed"),
    EXPIRED("expired"),
    DELETED("deleted");

    private static final Map<String, SessionTranscriptStatus> LEGACY_ALIASES = Map.of(
            "started", RECORDING,
            "uploading", RECORDING,
            "finalizing", PROCESSING,
            "completed", READY
    );

    private final String value;

    SessionTranscriptStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SessionTranscriptStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim().toLowerCase();
        SessionTranscriptStatus legacy = LEGACY_ALIASES.get(normalized);
        if (legacy != null) {
            return legacy;
        }
        return Arrays.stream(values())
                .filter(v -> v.value.equalsIgnoreCase(normalized) || v.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid transcript status: " + value));
    }
}
