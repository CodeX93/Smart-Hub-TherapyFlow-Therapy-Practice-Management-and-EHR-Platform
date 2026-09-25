package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Therapy modality - how therapy is delivered
 * Represents the type/format of therapy session
 */
public enum ClientType {

    INDIVIDUAL("Individual", "One-on-one therapy with therapist"),
    COUPLE("Couple", "Therapy for couples/partners"),
    FAMILY("Family", "Therapy involving family members"),
    GROUP("Group", "Therapy in a group setting");

    private final String displayName;
    private final String description;

    ClientType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    @JsonCreator
    public static ClientType fromValue(String value) {
        return Arrays.stream(ClientType.values())
                .filter(modality -> modality.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid therapy modality: " + value)
                );
    }

    /* ---------- Domain Logic ---------- */

    public boolean isGroupBased() {
        return this == GROUP || this == FAMILY;
    }

    public boolean requiresMultipleParticipants() {
        return this == COUPLE || this == FAMILY || this == GROUP;
    }

    public int getMinimumParticipants() {
        return switch (this) {
            case INDIVIDUAL -> 1;
            case COUPLE -> 2;
            case FAMILY -> 2;
            case GROUP -> 3;
        };
    }
}
