package com.smart.therapy.flow.client.enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Marital status options for clients
 */
public enum MaritalStatus {
    SINGLE("Single"),
    MARRIED("Married"),
    DIVORCED("Divorced"),
    WIDOWED("Widowed"),
    SEPARATED("Separated"),
    DOMESTIC_PARTNERSHIP("Domestic Partnership"),
    PREFER_NOT_TO_SAY("Prefer Not to Say");

    private final String displayName;

    MaritalStatus(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static MaritalStatus fromValue(String value) {
        return Arrays.stream(MaritalStatus.values())
                .filter(status -> status.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid marital status: " + value)
                );
    }
}

