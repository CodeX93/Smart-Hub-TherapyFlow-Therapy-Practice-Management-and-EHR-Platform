package com.smart.therapy.flow.session.util;

import com.smart.therapy.flow.client.portal.enums.SessionHistoryScope;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.Objects;

/**
 * Single source of truth for portal session history scope rules.
 * <p>
 * Classification compares absolute {@link Instant}s ({@code sessionDate} + duration vs {@code now}).
 * The client {@link ZoneId} is resolved for portal display/stats boundaries; it does not shift
 * the instant comparison (the same moment is past or upcoming everywhere).
 */
public final class SessionHistoryClassifier {

    public static final int DEFAULT_DURATION_MINUTES = 60;

    private SessionHistoryClassifier() {
    }

    public static Instant sessionEnd(Session session) {
        Instant start = session != null ? session.getSessionDate() : null;
        if (start == null) {
            return Instant.EPOCH;
        }
        int durationMinutes = session.getDuration() != null ? session.getDuration() : DEFAULT_DURATION_MINUTES;
        return start.plus(durationMinutes, ChronoUnit.MINUTES);
    }

    public static boolean isPast(Session session, Instant now) {
        Objects.requireNonNull(now, "now is required");
        if (session == null || session.getSessionDate() == null) {
            return false;
        }
        String status = session.getStatus();
        if (isTerminalStatus(status)) {
            return true;
        }
        return sessionEnd(session).isBefore(now);
    }

    public static boolean isUpcoming(Session session, Instant now) {
        Objects.requireNonNull(now, "now is required");
        if (session == null || session.getSessionDate() == null) {
            return false;
        }
        String status = session.getStatus();
        if (!isActiveStatus(status)) {
            return false;
        }
        return !sessionEnd(session).isBefore(now);
    }

    public static boolean matchesScope(Session session, SessionHistoryScope scope, Instant now) {
        return matchesScope(session, scope, now, ZoneId.of("UTC"));
    }

    public static boolean matchesScope(
            Session session, SessionHistoryScope scope, Instant now, ZoneId clientZone) {
        Objects.requireNonNull(scope, "scope is required");
        Objects.requireNonNull(clientZone, "clientZone is required");
        return switch (scope) {
            case PAST -> isPast(session, now);
            case UPCOMING -> isUpcoming(session, now);
        };
    }

    private static boolean isTerminalStatus(String status) {
        return SystemOptionKeyMatcher.matchesAny(status, "completed", "cancelled", "no-show", "no_show");
    }

    private static boolean isActiveStatus(String status) {
        return status != null
                && !isTerminalStatus(status)
                && SystemOptionKeyMatcher.matchesAny(status, "scheduled", "confirmed", "rescheduling", "in_progress", "in-progress");
    }

    public static Comparator<Session> comparator(SessionHistoryScope scope) {
        Comparator<Session> byStart = Comparator.comparing(
                Session::getSessionDate,
                Comparator.nullsLast(Comparator.naturalOrder()));
        return scope == SessionHistoryScope.UPCOMING ? byStart : byStart.reversed();
    }
}
