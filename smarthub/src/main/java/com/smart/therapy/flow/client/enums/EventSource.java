package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Source of an event (where it originated)
 */
public enum EventSource {

    WEB_APP("Web Application"),
    MOBILE_APP("Mobile Application"),
    API("API"),
    SYSTEM("System"),
    ADMIN("Administrator"),
    CLIENT_PORTAL("Client Portal"),
    IMPORT("Data Import"),
    INTEGRATION("Third-Party Integration");

    private final String displayName;

    EventSource(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static EventSource fromValue(String value) {
        return Arrays.stream(EventSource.values())
                .filter(source -> source.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid event source: " + value)
                );
    }
}
