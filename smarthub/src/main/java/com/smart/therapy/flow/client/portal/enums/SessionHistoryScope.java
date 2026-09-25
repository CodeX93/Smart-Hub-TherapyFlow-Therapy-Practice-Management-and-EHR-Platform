package com.smart.therapy.flow.client.portal.enums;

import com.smart.therapy.flow.common.exception.BadRequestException;

import java.util.Locale;

/**
 * Portal session history tab filter ({@code GET /api/v1/portal/me/sessions-history}).
 */
public enum SessionHistoryScope {
    UPCOMING,
    PAST;

    public static SessionHistoryScope from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Invalid scope. Use upcoming or past.");
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "upcoming" -> UPCOMING;
            case "past" -> PAST;
            default -> throw new BadRequestException("Invalid scope. Use upcoming or past.");
        };
    }
}
