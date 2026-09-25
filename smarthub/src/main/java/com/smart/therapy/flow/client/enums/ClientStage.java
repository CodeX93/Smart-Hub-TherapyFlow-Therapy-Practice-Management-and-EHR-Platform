package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Client's stage in the therapy process
 */
public enum ClientStage {
    INTAKE("Intake", "Initial intake and paperwork"),
    ASSESSMENT("Assessment", "Assessment and evaluation phase"),
    ACTIVE_TREATMENT("Active Treatment", "Actively receiving treatment"),
    MAINTENANCE("Maintenance", "Maintenance phase"),
    DISCHARGE_PLANNING("Discharge Planning", "Planning for discharge"),
    DISCHARGE("Discharge", "Discharged from services"),
    FOLLOW_UP("Follow-Up", "Post-discharge follow-up"),
    CLOSED("Closed", "Client file closed");

    private final String displayName;
    private final String description;

    ClientStage(String displayName, String description) {
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
    public static ClientStage fromValue(String value) {
        return Arrays.stream(ClientStage.values())
                .filter(stage -> stage.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid client stage: " + value)
                );
    }

    public boolean canMoveTo(ClientStage next) {
        if (next == CLOSED) {
            return true;
        }
        return switch (this) {
            case INTAKE -> next == ASSESSMENT;
            case ASSESSMENT -> next == ACTIVE_TREATMENT || next == DISCHARGE;
            case ACTIVE_TREATMENT -> next == MAINTENANCE || next == DISCHARGE_PLANNING;
            case MAINTENANCE -> next == DISCHARGE_PLANNING;
            case DISCHARGE_PLANNING -> next == DISCHARGE;
            case DISCHARGE -> next == FOLLOW_UP;
            case FOLLOW_UP, CLOSED -> false;
        };
    }
}

