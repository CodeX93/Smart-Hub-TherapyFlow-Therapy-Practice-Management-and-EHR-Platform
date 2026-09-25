package com.smart.therapy.flow.assessment.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Assessment assignment status values following the workflow:
 * pending → client_in_progress → waiting_for_review → therapist_completed → waiting_for_therapist → completed
 */
public enum AssessmentStatus {
    PENDING("pending", "Newly assigned, client hasn't started"),
    CLIENT_IN_PROGRESS("client_in_progress", "Client is completing the assessment"),
    WAITING_FOR_REVIEW("waiting_for_review", "Client submitted, waiting for therapist review"),
    THERAPIST_COMPLETED("therapist_completed", "Therapist completed their sections"),
    WAITING_FOR_THERAPIST("waiting_for_therapist", "Report generated, waiting for therapist finalization"),
    COMPLETED("completed", "Assessment fully completed and report finalized");

    private final String value;
    private final String description;

    AssessmentStatus(String value, String description) {
        this.value = value;
        this.description = description;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Short label suitable for UI badges and list views.
     */
    public String getDisplayLabel() {
        return switch (this) {
            case PENDING -> "Pending";
            case CLIENT_IN_PROGRESS -> "In Progress";
            case WAITING_FOR_REVIEW -> "Submitted";
            case THERAPIST_COMPLETED -> "Therapist Reviewed";
            case WAITING_FOR_THERAPIST -> "Awaiting Report";
            case COMPLETED -> "Completed";
        };
    }

    /**
     * Resolve a UI label for a raw status value without throwing on unknown values.
     */
    public static String displayLabelFor(String value) {
        if (value == null || value.isBlank()) {
            return "Unknown";
        }
        return Arrays.stream(AssessmentStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value.trim()))
                .findFirst()
                .map(AssessmentStatus::getDisplayLabel)
                .orElse(value);
    }

    @JsonCreator
    public static AssessmentStatus fromValue(String value) {
        return Arrays.stream(AssessmentStatus.values())
                .filter(status -> status.value.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Invalid assessment status: " + value));
    }

    /**
     * Check if status allows client to edit responses
     */
    public boolean allowsClientEdit() {
        return this == PENDING || this == CLIENT_IN_PROGRESS;
    }

    /**
     * Check if status allows therapist to edit
     */
    public boolean allowsTherapistEdit() {
        return this != COMPLETED;
    }

    /**
     * Check if assessment is in a final state
     */
    public boolean isFinal() {
        return this == COMPLETED;
    }

    /**
     * Check if assessment can be edited
     */
    public boolean canBeEdited() {
        return !isFinal();
    }
}
