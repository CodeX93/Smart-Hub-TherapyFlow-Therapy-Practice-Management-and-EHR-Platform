package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Types of events tracked in client history
 */
public enum ClientEventType {

    // Lifecycle events
    CREATED("Client Created"),
    UPDATED("Client Updated"),
    DELETED("Client Deleted"),
    RESTORED("Client Restored"),

    // Status changes
    STATUS_CHANGED("Status Changed"),
    STAGE_CHANGED("Stage Changed"),
    FILE_CLOSED("File Closed"),
    FILE_REOPENED("File Reopened"),

    // Assignment events
    THERAPIST_ASSIGNED("Therapist Assigned"),
    THERAPIST_CHANGED("Therapist Changed"),
    THERAPIST_UNASSIGNED("Therapist Unassigned"),

    // Portal events
    PORTAL_ACTIVATED("Portal Access Activated"),
    PORTAL_DEACTIVATED("Portal Access Deactivated"),
    PORTAL_LOGIN("Portal Login"),
    PORTAL_PASSWORD_RESET("Password Reset Requested"),

    // Clinical events
    SESSION_SCHEDULED("Session Scheduled"),
    SESSION_COMPLETED("Session Completed"),
    SESSION_CANCELLED("Session Cancelled"),

    // Document events
    DOCUMENT_UPLOADED("Document Uploaded"),
    DOCUMENT_SHARED("Document Shared"),
    FORM_ASSIGNED("Form Assigned"),
    FORM_COMPLETED("Form Completed"),
    CONSENT_GRANTED("Consent Granted"),
    CONSENT_WITHDRAWN("Consent Withdrawn"),

    // Administrative
    INSURANCE_UPDATED("Insurance Information Updated"),
    INSURANCE_ADDED("Insurance Information Added"),
    REFERRAL_UPDATED("Referral Information Updated"),
    REFERRAL_ADDED("Referral Information Added"),
    EMPLOYMENT_UPDATED("Employment Information Updated"),
    EMPLOYMENT_ADDED("Employment Information Added"),
    CONTACT_UPDATED("Contact Information Updated"),
    CONTACT_ADDED("Contact Information Added"),
    CONTACT_REMOVED("Contact Information Removed"),
    ADDRESS_UPDATED("Address Information Updated"),
    ADDRESS_ADDED("Address Information Added"),
    ADDRESS_REMOVED("Address Information Removed"),
    EMERGENCY_CONTACT_UPDATED("Emergency Contact Updated"),

    // Follow-up
    FOLLOW_UP_SCHEDULED("Follow-Up Scheduled"),
    FOLLOW_UP_COMPLETED("Follow-Up Completed"),

    // Other
    NOTE_ADDED("Note Added"),
    DUPLICATE_MARKED("Marked as Duplicate"),
    DUPLICATE_UNMARKED("Unmarked as Duplicate");

    private final String displayName;

    ClientEventType(String displayName) {
        this.displayName = displayName;
    }

    @JsonValue
    public String getDisplayName() {
        return displayName;
    }

    @JsonCreator
    public static ClientEventType fromValue(String value) {
        return Arrays.stream(ClientEventType.values())
                .filter(e -> e.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid client event type: " + value)
                );
    }
}
