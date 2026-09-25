package com.smart.therapy.flow.common.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Base for PATCH/PUT bodies where {@code null} means "clear this field" and omitted keys mean "leave unchanged".
 */
public abstract class PatchAwareRequest {

    @JsonIgnore
    private final Set<String> presentFields = new HashSet<>();

    public void markFieldPresent(String jsonFieldName) {
        if (jsonFieldName != null && !jsonFieldName.isBlank()) {
            presentFields.add(jsonFieldName);
        }
    }

    public boolean isFieldPresent(String jsonFieldName) {
        return presentFields.contains(jsonFieldName);
    }

    public boolean isAnyFieldPresent(String... jsonFieldNames) {
        if (jsonFieldNames == null) {
            return false;
        }
        for (String name : jsonFieldNames) {
            if (isFieldPresent(name)) {
                return true;
            }
        }
        return false;
    }

    public Set<String> getPresentFields() {
        return Collections.unmodifiableSet(presentFields);
    }
}
