package com.smart.therapy.flow.unit.session;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Documents why scheduling "This Month" must use practice timezone, not browser.
 * Prod tenant_50 (2026-09-04): America/Toronto Sept=152, Asia/Karachi Sept=163.
 * Overview/calendar now default to practice TZ (else America/New_York) when timezone is omitted.
 */
class SessionOverviewTimezoneCountTest {

    @Test
    void septemberBoundarySessionsDifferBetweenPracticeAndBrowserZones() {
        ZoneId practice = ZoneId.of("America/Toronto");
        ZoneId browser = ZoneId.of("Asia/Karachi");

        List<Instant> sessions = List.of(
                Instant.parse("2026-08-31T12:00:00Z"),
                Instant.parse("2026-08-31T20:00:00Z"),
                Instant.parse("2026-09-01T02:00:00Z"),
                Instant.parse("2026-09-15T15:00:00Z"),
                Instant.parse("2026-09-30T22:00:00Z")
        );

        long practiceSept = countInMonth(sessions, 2026, 9, practice);
        long browserSept = countInMonth(sessions, 2026, 9, browser);

        assertEquals(2, practiceSept);
        assertEquals(3, browserSept);
        assertNotEquals(practiceSept, browserSept);
    }

    @Test
    void practiceSeptemberRangeStartIsEasternMidnightAsUtc() {
        ZoneId practice = ZoneId.of("America/Toronto");
        Instant start = LocalDate.of(2026, 9, 1).atStartOfDay(practice).toInstant();
        assertEquals(Instant.parse("2026-09-01T04:00:00Z"), start);
    }

    private static long countInMonth(List<Instant> sessions, int year, int month, ZoneId zone) {
        return sessions.stream()
                .map(i -> ZonedDateTime.ofInstant(i, zone))
                .filter(z -> z.getYear() == year && z.getMonthValue() == month)
                .count();
    }
}
