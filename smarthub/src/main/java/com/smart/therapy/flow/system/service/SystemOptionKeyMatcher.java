package com.smart.therapy.flow.system.service;

import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Locale;

public final class SystemOptionKeyMatcher {

    private SystemOptionKeyMatcher() {
    }

    public static boolean matchesAny(String actual, String... expectedKeys) {
        if (!StringUtils.hasText(actual) || expectedKeys == null) {
            return false;
        }
        String normalizedActual = normalize(actual);
        return Arrays.stream(expectedKeys)
                .filter(StringUtils::hasText)
                .map(SystemOptionKeyMatcher::normalize)
                .anyMatch(normalizedActual::equals);
    }

    public static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replace('-', '_');
    }
}
