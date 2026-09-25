package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Frequency of service delivery
 */
public enum ServiceFrequency {

    DAILY("Daily"),
    TWICE_WEEKLY("Twice Weekly"),
    WEEKLY("Weekly"),
    BIWEEKLY("Biweekly (Every 2 Weeks)"),
    MONTHLY("Monthly"),
    QUARTERLY("Quarterly"),
    AS_NEEDED("As Needed"),
    ONE_TIME("One Time");

    private final String displayName;

    ServiceFrequency(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ServiceFrequency fromValue(String value) {
        return Arrays.stream(ServiceFrequency.values())
                .filter(f -> f.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid service frequency: " + value)
                );
    }
}
