package com.smart.therapy.flow.client.enums;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Gender options for clients
 * Inclusive representation following mental health best practices
 */
public enum Gender {
    MALE("Male"),
    FEMALE("Female"),
    NON_BINARY("Non-Binary"),
    TRANSGENDER("Transgender"),
    PREFER_NOT_TO_SAY("Prefer Not to Say"),
    OTHER("Other");

    private final String displayName;

    Gender(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static Gender fromValue(String value) {
        return Arrays.stream(Gender.values())
                .filter(g -> g.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> 
                    new IllegalArgumentException("Invalid gender: " + value)
                );
    }
}   

