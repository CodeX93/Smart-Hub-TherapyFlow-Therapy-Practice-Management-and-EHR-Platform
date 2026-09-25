package com.smart.therapy.flow.billing.service;

import com.smart.therapy.flow.session.enums.SessionStatus;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

/**
 * Maps {@link SessionStatus} values to invoice-policy appointment-status option keys.
 */
public final class SessionAppointmentStatusMapper {

    private SessionAppointmentStatusMapper() {
    }

    public static Set<String> toPolicyAppointmentStatusKeys(String status) {
        Set<String> keys = new LinkedHashSet<>();
        if (!StringUtils.hasText(status)) {
            return keys;
        }
        keys.add(normalize(status));
        try {
            keys.addAll(toPolicyAppointmentStatusKeys(SessionStatus.fromValue(status)));
        } catch (IllegalArgumentException ex) {
            addLegacyAliases(keys, status);
        }
        keys.remove("");
        return keys;
    }

    public static Set<String> toPolicyAppointmentStatusKeys(SessionStatus status) {
        Set<String> keys = new LinkedHashSet<>();
        if (status == null) {
            return keys;
        }
        keys.add(normalize(status.getValue()));
        keys.add(normalize(status.name()));
        keys.add(normalize(status.getDisplayName()));

        if (status == SessionStatus.COMPLETED) {
            keys.add("show-up");
            keys.add("showup");
            keys.add("show_up");
        } else if (status == SessionStatus.NO_SHOW) {
            keys.add("no-show");
            keys.add("noshow");
            keys.add("no_show");
        } else if (status == SessionStatus.RESCHEDULING) {
            keys.add("rescheduled");
        } else if (status == SessionStatus.CANCELLED) {
            keys.add("canceled");
        }
        return keys;
    }

    private static void addLegacyAliases(Set<String> keys, String status) {
        String normalized = normalize(status);
        String hyphenated = normalized.replace('_', '-');
        if ("completed".equals(normalized) || "show-up".equals(hyphenated) || "showup".equals(normalized)) {
            keys.add("completed");
            keys.add("show-up");
            keys.add("showup");
        } else if ("no-show".equals(hyphenated) || "noshow".equals(normalized) || "no_show".equals(normalized)) {
            keys.add("no-show");
            keys.add("noshow");
            keys.add("no_show");
        } else if ("cancelled".equals(normalized) || "canceled".equals(normalized)) {
            keys.add("cancelled");
            keys.add("canceled");
        } else if ("rescheduling".equals(normalized) || "rescheduled".equals(normalized)) {
            keys.add("rescheduling");
            keys.add("rescheduled");
        }
    }

    private static String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
