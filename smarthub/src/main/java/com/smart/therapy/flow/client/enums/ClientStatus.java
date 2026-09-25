package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;


/**
 * Client status in the therapy workflow
 */
public enum ClientStatus {
    ACTIVE("Active", "Client is currently receiving services"),
    INACTIVE("Inactive", "Client is not currently receiving services"),
    PENDING("Pending", "Client intake is pending"),
    DISCHARGED("Discharged", "Client has been formally discharged"),
    ON_HOLD("On Hold", "Services temporarily paused"),
    WAITLIST("Waitlist", "Client is on waiting list");

    private final String displayName;
    private final String description;

    ClientStatus(String displayName, String description) {
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
    public static ClientStatus fromValue(String value) {
        return Arrays.stream(ClientStatus.values())
                .filter(status -> status.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid client status: " + value)
                );
    }

    /* ---------- Domain Logic ---------- */

    public boolean isActive() {
        return this == ACTIVE;
    }

    public boolean canReceiveServices() {
        return this == ACTIVE || this == ON_HOLD;
    }

    public boolean canTransitionTo(ClientStatus next) {
        return switch (this) {
            case PENDING -> next == ACTIVE || next == WAITLIST || next == INACTIVE;
            case ACTIVE -> next == ON_HOLD || next == DISCHARGED || next == INACTIVE;
            case ON_HOLD -> next == ACTIVE || next == DISCHARGED || next == INACTIVE;
            case WAITLIST -> next == ACTIVE || next == INACTIVE;
            case INACTIVE -> next == ACTIVE;
            case DISCHARGED -> false;
        };
    }

    public boolean isFileClosed() {
        return this == INACTIVE;
    }
}

