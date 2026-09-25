package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Priority levels for follow-ups, tasks, and urgency indicators
 */
public enum Priority {

    LOW("Low", 1),
    MEDIUM("Medium", 2),
    HIGH("High", 3),
    URGENT("Urgent", 4);

    private final String displayName;
    private final int level;

    Priority(String displayName, int level) {
        this.displayName = displayName;
        this.level = level;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    public int getLevel() {
        return level;
    }

    @JsonCreator
    public static Priority fromValue(String value) {
        return Arrays.stream(Priority.values())
                .filter(p -> p.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid priority: " + value)
                );
    }

    /* ---------- Domain Logic ---------- */

    public boolean isHigherThan(Priority other) {
        return other == null || this.level > other.level;
    }

    public boolean requiresClinicalPrivilege() {
        return this == URGENT;
    }
}
