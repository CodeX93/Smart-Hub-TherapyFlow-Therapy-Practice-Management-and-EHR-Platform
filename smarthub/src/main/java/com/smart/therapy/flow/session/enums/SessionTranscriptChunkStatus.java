package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum SessionTranscriptChunkStatus {
    RECEIVED("received"),
    SILENT("silent"),
    FAILED("failed");

    private final String value;

    SessionTranscriptChunkStatus(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static SessionTranscriptChunkStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        return Arrays.stream(values())
                .filter(v -> v.value.equalsIgnoreCase(value) || v.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid transcript chunk status: " + value));
    }
}

