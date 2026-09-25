package com.smart.therapy.flow.common.dto;

import org.springframework.util.StringUtils;

import java.util.function.Consumer;

public final class PatchUpdates {

    private PatchUpdates() {
    }

    public static <T> void apply(PatchAwareRequest request, String field, T value, Consumer<T> setter) {
        if (request != null && request.isFieldPresent(field)) {
            setter.accept(value);
        }
    }

    public static void applyString(PatchAwareRequest request, String field, String value, Consumer<String> setter) {
        if (request != null && request.isFieldPresent(field)) {
            setter.accept(StringUtils.hasText(value) ? value.trim() : null);
        }
    }

    public static void applyTrimmedOrNull(PatchAwareRequest request, String field, String value, Consumer<String> setter) {
        applyString(request, field, value, setter);
    }
}
