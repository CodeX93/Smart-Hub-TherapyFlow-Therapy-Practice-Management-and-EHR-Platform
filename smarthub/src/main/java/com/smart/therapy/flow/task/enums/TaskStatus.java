package com.smart.therapy.flow.task.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Task lifecycle status.
 */
public enum TaskStatus {
    PENDING("pending", "Pending"),
    IN_PROGRESS("in_progress", "In Progress"),
    COMPLETED("completed", "Completed"),
    CANCELLED("cancelled", "Cancelled"),
    OVERDUE("overdue", "Overdue");

    private final String value;
    private final String displayName;

    TaskStatus(String value, String displayName) {
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
    public static TaskStatus fromValue(String value) {
        if (value == null) return null;
        return Arrays.stream(TaskStatus.values())
                .filter(s -> s.value.equalsIgnoreCase(value) || s.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid task status: " + value));
    }
}
