package com.smart.therapy.flow.session.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * ClientHub-compatible calendar day/month bounds in a practice timezone.
 * Matches {@code DATE(session_date)} filtering: local calendar days expressed as Instant
 * ranges for UTC-stored {@code session_date} values.
 */
public final class CalendarDateBounds {

    private CalendarDateBounds() {
    }

    /**
     * Snap an Instant filter start to the start of the intended local calendar day in {@code zone}.
     * UTC midnights are treated as date-only values from the FE (ClientHub date pickers).
     */
    public static Instant startOfLocalDay(Instant startDate, ZoneId zone) {
        if (startDate == null) {
            return null;
        }
        Objects.requireNonNull(zone, "zone");
        return intendedCalendarDate(startDate, zone).atStartOfDay(zone).toInstant();
    }

    /**
     * Inclusive end Instant for specs that use {@code sessionDate <= endDate}.
     * <p>
     * UTC or practice-local midnight is treated as an exclusive next-day boundary
     * (common for month/week end Instant from the FE).
     */
    public static Instant endOfLocalDayInclusive(Instant endDate, ZoneId zone) {
        if (endDate == null) {
            return null;
        }
        Objects.requireNonNull(zone, "zone");
        LocalDate day = intendedCalendarDate(endDate, zone);
        if (isMidnight(endDate.atZone(ZoneOffset.UTC)) || isMidnight(endDate.atZone(zone))) {
            day = day.minusDays(1);
        }
        return endOfDayInclusive(day, zone);
    }

    /** Half-open start Instant for a local calendar date. */
    public static Instant startOfDay(LocalDate date, ZoneId zone) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        return date.atStartOfDay(zone).toInstant();
    }

    /** Inclusive end Instant (last nanos) for a local calendar date. */
    public static Instant endOfDayInclusive(LocalDate date, ZoneId zone) {
        Objects.requireNonNull(date, "date");
        Objects.requireNonNull(zone, "zone");
        return date.plusDays(1).atStartOfDay(zone).toInstant().minusNanos(1);
    }

    /**
     * Resolve which calendar date an Instant filter refers to.
     * Date-only {@code Z} midnights keep their UTC date; otherwise use {@code zone}'s local date.
     */
    public static LocalDate intendedCalendarDate(Instant instant, ZoneId zone) {
        Objects.requireNonNull(instant, "instant");
        Objects.requireNonNull(zone, "zone");
        ZonedDateTime utc = instant.atZone(ZoneOffset.UTC);
        if (isMidnight(utc)) {
            return utc.toLocalDate();
        }
        return instant.atZone(zone).toLocalDate();
    }

    private static boolean isMidnight(ZonedDateTime zdt) {
        return zdt.toLocalTime().equals(LocalTime.MIDNIGHT) && zdt.getNano() == 0;
    }
}
