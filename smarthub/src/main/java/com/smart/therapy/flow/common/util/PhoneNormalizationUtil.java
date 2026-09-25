package com.smart.therapy.flow.common.util;

import org.springframework.util.StringUtils;

/**
 * Normalizes phone numbers to E.164 for SMS delivery.
 * Refuses ambiguous numbers rather than guessing destinations.
 */
public final class PhoneNormalizationUtil {

    private PhoneNormalizationUtil() {
    }

    /**
     * Normalize a raw phone string to E.164 format.
     *
     * @param raw user-entered phone value
     * @return E.164 string (e.g. +15195551234) or null if ambiguous/invalid
     */
    public static String normalizePhoneE164(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }

        String trimmed = raw.trim();
        if (trimmed.startsWith("+")) {
            String digits = trimmed.substring(1).replaceAll("\\D", "");
            if (digits.length() >= 8 && digits.length() <= 15) {
                return "+" + digits;
            }
            return null;
        }

        String digits = trimmed.replaceAll("\\D", "");
        if (digits.length() == 10) {
            return "+1" + digits;
        }
        if (digits.length() == 11 && digits.startsWith("1")) {
            return "+" + digits;
        }
        return null;
    }
}
