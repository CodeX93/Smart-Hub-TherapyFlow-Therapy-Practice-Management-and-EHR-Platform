package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.session.util.CalendarDateBounds;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * ClientHub DATE(session_date) parity for calendar Instant bounds in practice TZ.
 */
class CalendarDateBoundsTest {

    private static final ZoneId TORONTO = ZoneId.of("America/Toronto");

    @Test
    void septemberMonthStartIsEasternMidnight() {
        Instant start = CalendarDateBounds.startOfDay(LocalDate.of(2026, 9, 1), TORONTO);
        assertEquals(Instant.parse("2026-09-01T04:00:00Z"), start);
    }

    @Test
    void septemberMonthEndInclusiveIncludesLastEasternNanos() {
        Instant end = CalendarDateBounds.endOfDayInclusive(LocalDate.of(2026, 9, 30), TORONTO);
        Instant octoberStart = LocalDate.of(2026, 10, 1).atStartOfDay(TORONTO).toInstant();
        assertEquals(octoberStart.minusNanos(1), end);
        // Late evening Sep 30 ET is still inside the month window
        Instant lateSep30 = Instant.parse("2026-10-01T03:30:00Z"); // 23:30 EDT Sep 30
        assertTrue(!lateSep30.isAfter(end));
        assertTrue(lateSep30.isBefore(octoberStart));
    }

    @Test
    void utcMidnightStartKeepsCalendarDateInPracticeZone() {
        // FE date-only Sept 1 → practice Sept 1 00:00 (not Aug 31)
        Instant snapped = CalendarDateBounds.startOfLocalDay(Instant.parse("2026-09-01T00:00:00Z"), TORONTO);
        assertEquals(Instant.parse("2026-09-01T04:00:00Z"), snapped);
        assertEquals(LocalDate.of(2026, 9, 1), snapped.atZone(TORONTO).toLocalDate());
    }

    @Test
    void practiceAlignedStartIsIdempotent() {
        Instant already = LocalDate.of(2026, 9, 1).atStartOfDay(TORONTO).toInstant();
        assertEquals(already, CalendarDateBounds.startOfLocalDay(already, TORONTO));
    }

    @Test
    void exclusiveMonthEndMidnightDoesNotBleedIntoNextMonth() {
        Instant exclusiveOct1Utc = Instant.parse("2026-10-01T00:00:00Z");
        Instant inclusiveEnd = CalendarDateBounds.endOfLocalDayInclusive(exclusiveOct1Utc, TORONTO);
        assertEquals(CalendarDateBounds.endOfDayInclusive(LocalDate.of(2026, 9, 30), TORONTO), inclusiveEnd);

        Instant exclusiveOct1Eastern = LocalDate.of(2026, 10, 1).atStartOfDay(TORONTO).toInstant();
        assertEquals(
                CalendarDateBounds.endOfDayInclusive(LocalDate.of(2026, 9, 30), TORONTO),
                CalendarDateBounds.endOfLocalDayInclusive(exclusiveOct1Eastern, TORONTO));
    }

    @Test
    void inclusiveUtcEndOfDaySnapsToFullLocalDay() {
        Instant endOfSepUtc = Instant.parse("2026-09-30T23:59:59Z");
        Instant inclusiveEnd = CalendarDateBounds.endOfLocalDayInclusive(endOfSepUtc, TORONTO);
        assertEquals(CalendarDateBounds.endOfDayInclusive(LocalDate.of(2026, 9, 30), TORONTO), inclusiveEnd);
    }
}
