package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Type of service provided to client
 */
public enum ServiceType {

    PSYCHOTHERAPY("Psychotherapy", "Individual psychotherapy"),
    COUNSELING("Counseling", "General counseling services"),
    ASSESSMENT("Assessment", "Psychological assessment"),
    CONSULTATION("Consultation", "Professional consultation"),
    GROUP_THERAPY("Group Therapy", "Group therapy session"),
    FAMILY_THERAPY("Family Therapy", "Family therapy session"),
    COUPLES_THERAPY("Couples Therapy", "Couples therapy session"),
    CRISIS_INTERVENTION("Crisis Intervention", "Emergency crisis intervention"),
    CASE_MANAGEMENT("Case Management", "Case management services"),
    MEDICATION_MANAGEMENT("Medication Management", "Psychiatric medication management");

    private final String displayName;
    private final String description;

    ServiceType(String displayName, String description) {
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
    public static ServiceType fromValue(String value) {
        return Arrays.stream(ServiceType.values())
                .filter(type -> type.displayName.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid service type: " + value)
                );
    }
}
