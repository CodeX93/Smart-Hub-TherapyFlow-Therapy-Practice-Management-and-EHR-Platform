package com.smart.therapy.flow.common.util;

import java.util.Arrays;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Shared helpers for parsing API enum values from constant names or display labels.
 */
public final class EnumParsing {

    private EnumParsing() {
    }

    public static <E extends Enum<E>> E parse(
            Class<E> enumType,
            String value,
            Function<E, String> displayNameFn,
            String fieldLabel) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        String normalizedToken = normalizeToken(trimmed);

        return Arrays.stream(enumType.getEnumConstants())
                .filter(candidate -> matches(candidate, trimmed, normalizedToken, displayNameFn))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Invalid " + fieldLabel + ": '" + value + "'. Allowed values: "
                                + allowedValues(enumType, displayNameFn) + "."));
    }

    public static <E extends Enum<E>> String allowedValues(Class<E> enumType, Function<E, String> displayNameFn) {
        return Arrays.stream(enumType.getEnumConstants())
                .map(candidate -> candidate.name() + " (\"" + displayNameFn.apply(candidate) + "\")")
                .collect(Collectors.joining(", "));
    }

    private static <E extends Enum<E>> boolean matches(
            E candidate,
            String raw,
            String normalizedToken,
            Function<E, String> displayNameFn) {
        if (candidate.name().equalsIgnoreCase(raw)) {
            return true;
        }
        if (normalizeToken(candidate.name()).equalsIgnoreCase(normalizedToken)) {
            return true;
        }
        String displayName = displayNameFn.apply(candidate);
        return displayName != null && displayName.equalsIgnoreCase(raw);
    }

    private static String normalizeToken(String value) {
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replace('-', '_')
                .replace(' ', '_');
    }
}
