package com.smart.therapy.flow.session.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;
import java.util.Locale;

/**
 * Session status enumeration
 */
public enum SessionStatus {
    SCHEDULED("scheduled", "Scheduled"),
    CONFIRMED("confirmed", "Confirmed"),
    IN_PROGRESS("in-progress", "In Progress"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled"),
    RESCHEDULING("rescheduling", "Rescheduling"),
    NO_SHOW("no-show", "No Show"),
    OVERDUE("overdue", "Overdue");

    private final String value;
    private final String displayName;

    SessionStatus(String value, String displayName) {
        this.value = value;
        this.displayName = displayName;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static SessionStatus fromValue(String value) {
        if (value == null) {
            return null;
        }
        String normalizedInput = normalize(value);
        return Arrays.stream(SessionStatus.values())
                .filter(status -> normalize(status.value).equals(normalizedInput)
                        || normalize(status.name()).equals(normalizedInput)
                        || normalize(status.displayName).equals(normalizedInput))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid session status: " + value));
    }

    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim()
                .toLowerCase(Locale.ROOT)
                .replace("-", "")
                .replace("_", "")
                .replace(" ", "");
    }

    public boolean isActive() {
        return this == SCHEDULED || this == CONFIRMED || this == IN_PROGRESS;
    }

    public boolean isCompleted() {
        return this == COMPLETED;
    }

    public boolean isCancelled() {
        return this == CANCELLED || this == NO_SHOW;
    }

    public boolean canBeEdited() {
        return this != COMPLETED && this != CANCELLED;
    }
}
