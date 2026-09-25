package com.smart.therapy.flow.unit.session;

import com.smart.therapy.flow.client.portal.enums.SessionHistoryScope;
import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.session.entity.Session;
import com.smart.therapy.flow.session.enums.SessionStatus;
import com.smart.therapy.flow.session.util.SessionHistoryClassifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SessionHistoryClassifier")
class SessionHistoryClassifierTest {

    private static final Instant NOW = Instant.parse("2026-06-11T12:00:00Z");

    @Test
    @DisplayName("Future confirmed session is upcoming")
    void futureConfirmedIsUpcoming() {
        Session session = session(27L, SessionStatus.CONFIRMED.getValue(), NOW.plus(5, ChronoUnit.DAYS), 60);
        assertThat(SessionHistoryClassifier.isUpcoming(session, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.isPast(session, NOW)).isFalse();
    }

    @Test
    @DisplayName("Past confirmed session is past")
    void pastConfirmedIsPast() {
        Session session = session(27L, SessionStatus.CONFIRMED.getValue(), NOW.minus(1, ChronoUnit.DAYS), 60);
        assertThat(SessionHistoryClassifier.isPast(session, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.isUpcoming(session, NOW)).isFalse();
    }

    @Test
    @DisplayName("Terminal statuses are always past")
    void terminalStatusesArePast() {
        for (SessionStatus status : List.of(
                SessionStatus.CANCELLED, SessionStatus.COMPLETED, SessionStatus.NO_SHOW)) {
            Session future = session(21L, status.getValue(), NOW.plus(10, ChronoUnit.DAYS), 60);
            assertThat(SessionHistoryClassifier.isPast(future, NOW)).isTrue();
            assertThat(SessionHistoryClassifier.isUpcoming(future, NOW)).isFalse();
        }
    }

    @Test
    @DisplayName("Past scheduled session is past by time")
    void pastScheduledIsPast() {
        Session session = session(11L, SessionStatus.SCHEDULED.getValue(), Instant.parse("2026-05-19T15:00:00Z"), 60);
        assertThat(SessionHistoryClassifier.isPast(session, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.isUpcoming(session, NOW)).isFalse();
    }

    @Test
    @DisplayName("Future rescheduling session is upcoming")
    void futureReschedulingIsUpcoming() {
        Session session = session(28L, SessionStatus.RESCHEDULING.getValue(), Instant.parse("2026-06-16T15:00:00Z"), 60);
        assertThat(SessionHistoryClassifier.isUpcoming(session, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.isPast(session, NOW)).isFalse();
    }

    @Test
    @DisplayName("Sort order: upcoming ASC, past DESC")
    void sortOrderPerScope() {
        Session earlier = session(1L, SessionStatus.CONFIRMED.getValue(), NOW.plus(1, ChronoUnit.DAYS), 60);
        Session later = session(2L, SessionStatus.CONFIRMED.getValue(), NOW.plus(3, ChronoUnit.DAYS), 60);

        List<Session> upcomingSorted = List.of(later, earlier).stream()
                .sorted(SessionHistoryClassifier.comparator(SessionHistoryScope.UPCOMING))
                .collect(Collectors.toList());
        assertThat(upcomingSorted).containsExactly(earlier, later);

        List<Session> pastSorted = List.of(earlier, later).stream()
                .sorted(SessionHistoryClassifier.comparator(SessionHistoryScope.PAST))
                .collect(Collectors.toList());
        assertThat(pastSorted).containsExactly(later, earlier);
    }

    @Test
    @DisplayName("Invalid scope throws 400")
    void invalidScopeThrowsBadRequest() {
        assertThatThrownBy(() -> SessionHistoryScope.from("all"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid scope");
        assertThatThrownBy(() -> SessionHistoryScope.from(""))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("matchesScope delegates to past/upcoming rules")
    void matchesScopeDelegates() {
        Session upcoming = session(28L, SessionStatus.RESCHEDULING.getValue(), NOW.plus(2, ChronoUnit.DAYS), 60);
        Session past = session(27L, SessionStatus.CONFIRMED.getValue(), NOW.minus(2, ChronoUnit.DAYS), 60);

        assertThat(SessionHistoryClassifier.matchesScope(upcoming, SessionHistoryScope.UPCOMING, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.matchesScope(upcoming, SessionHistoryScope.PAST, NOW)).isFalse();
        assertThat(SessionHistoryClassifier.matchesScope(past, SessionHistoryScope.PAST, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.matchesScope(past, SessionHistoryScope.UPCOMING, NOW)).isFalse();
    }

    @Test
    @DisplayName("Session ending exactly at now is upcoming")
    void sessionEndingAtNowIsUpcoming() {
        Instant start = NOW.minus(60, ChronoUnit.MINUTES);
        Session session = session(99L, SessionStatus.CONFIRMED.getValue(), start, 60);
        assertThat(SessionHistoryClassifier.sessionEnd(session)).isEqualTo(NOW);
        assertThat(SessionHistoryClassifier.isUpcoming(session, NOW)).isTrue();
        assertThat(SessionHistoryClassifier.isPast(session, NOW)).isFalse();
    }

    private static Session session(Long id, String status, Instant sessionDate, int durationMinutes) {
        Session session = Session.builder()
                .status(status)
                .sessionDate(sessionDate)
                .duration(durationMinutes)
                .build();
        session.setId(id);
        return session;
    }
}
