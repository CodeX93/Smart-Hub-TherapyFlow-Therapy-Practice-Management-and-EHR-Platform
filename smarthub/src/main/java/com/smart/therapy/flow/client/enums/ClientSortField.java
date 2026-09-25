package com.smart.therapy.flow.client.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

/**
 * Enum defining valid sort fields for client queries.
 * Maps frontend sort field names to database column names.
 */
public enum ClientSortField {
    CREATED_AT("createdAt", "created_at"),
    UPDATED_AT("updatedAt", "last_update_date"),
    FULL_NAME("fullName", "full_name"),
    STATUS("status", "status"),
    STAGE("stage", "stage"),
    CLIENT_ID("clientId", "client_id"),
    DATE_OF_BIRTH("dateOfBirth", "date_of_birth"),
    LAST_SESSION_DATE("lastSessionDate", "last_session_date");

    private final String apiField;
    private final String dbColumn;

    ClientSortField(String apiField, String dbColumn) {
        this.apiField = apiField;
        this.dbColumn = dbColumn;
    }

    @JsonValue
    public String getApiField() {
        return apiField;
    }

    public String getDbColumn() {
        return dbColumn;
    }

    /**
     * Get the database column name for this sort field.
     * For JPA, we use the entity field name (camelCase), not the DB column name.
     */
    public String getEntityField() {
        return apiField;
    }

    @JsonCreator
    public static ClientSortField fromValue(String value) {
        if (value == null || value.trim().isEmpty()) {
            return CREATED_AT; // Default
        }
        
        return Arrays.stream(ClientSortField.values())
                .filter(field -> field.apiField.equalsIgnoreCase(value))
                .findFirst()
                .orElseThrow(() -> 
                    new IllegalArgumentException("Invalid sort field: " + value + 
                        ". Valid values: " + Arrays.toString(ClientSortField.values()))
                );
    }

    /**
     * Check if the given string is a valid sort field.
     */
    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true; // Empty is valid (will use default)
        }
        return Arrays.stream(ClientSortField.values())
                .anyMatch(field -> field.apiField.equalsIgnoreCase(value));
    }
}

