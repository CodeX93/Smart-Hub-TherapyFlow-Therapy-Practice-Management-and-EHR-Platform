package com.smart.therapy.flow.unit.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.session.service.RecurrenceDateExpander;
import com.smart.therapy.flow.session.service.RecurrenceDateExpander.ExpandedOccurrence;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("RecurrenceDateExpander Unit Tests")
class RecurrenceDateExpanderTest {

    private static final ZoneId EASTERN = ZoneId.of("America/New_York");
    private static final ZoneId KARACHI = ZoneId.of("Asia/Karachi");

    private RecurrenceDateExpander expander;

    @BeforeEach
    void setUp() {
        expander = new RecurrenceDateExpander();
    }

    @Test
    @DisplayName("Should expand weekly recurrence on Mon/Wed every week")
    void expandWeeklyMonWed() {
        RecurrenceRuleRequest request = weeklyRule(
                LocalDate.of(2026, 6, 8),
                "14:00",
                EASTERN,
                List.of(1, 3),
                1,
                RecurrenceEndMode.COUNT,
                4,
                null);

        List<ExpandedOccurrence> occurrences = expander.expand(request, EASTERN);

        assertThat(occurrences).hasSize(4);
        assertThat(occurrences.get(0).localDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(occurrences.get(1).localDate()).isEqualTo(LocalDate.of(2026, 6, 10));
        assertThat(occurrences.get(2).localDate()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(occurrences.get(3).localDate()).isEqualTo(LocalDate.of(2026, 6, 17));

        ZonedDateTime first = occurrences.get(0).utcDate().atZone(EASTERN);
        assertThat(first.getHour()).isEqualTo(14);
        assertThat(first.getMinute()).isEqualTo(0);
    }

    @Test
    @DisplayName("Should expand weekly recurrence using provided timezone")
    void expandWeeklyWithTimezone() {
        RecurrenceRuleRequest request = weeklyRule(
                LocalDate.of(2026, 6, 16),
                "11:00",
                KARACHI,
                List.of(2),
                1,
                RecurrenceEndMode.COUNT,
                4,
                null);

        List<ExpandedOccurrence> occurrences = expander.expand(request, KARACHI);

        assertThat(occurrences).hasSize(4);
        assertThat(occurrences.get(0).localDate()).isEqualTo(LocalDate.of(2026, 6, 16));
        assertThat(occurrences.get(0).sessionTime()).isEqualTo("11:00");
        assertThat(occurrences.get(0).utcDate()).isEqualTo(Instant.parse("2026-06-16T06:00:00Z"));
        assertThat(occurrences.get(1).localDate()).isEqualTo(LocalDate.of(2026, 6, 23));
    }

    @Test
    @DisplayName("Should respect every-2-weeks interval")
    void expandWeeklyEveryTwoWeeks() {
        RecurrenceRuleRequest request = weeklyRule(
                LocalDate.of(2026, 6, 8),
                "14:00",
                EASTERN,
                List.of(1),
                2,
                RecurrenceEndMode.COUNT,
                2,
                null);

        List<ExpandedOccurrence> occurrences = expander.expand(request, EASTERN);

        assertThat(occurrences).hasSize(2);
        assertThat(occurrences.get(0).localDate()).isEqualTo(LocalDate.of(2026, 6, 8));
        assertThat(occurrences.get(1).localDate()).isEqualTo(LocalDate.of(2026, 6, 22));
    }

    @Test
    @DisplayName("Should expand monthly recurrence every two months")
    void expandMonthlyEveryTwoMonths() {
        RecurrenceRuleRequest request = monthlyRule(
                LocalDate.of(2026, 1, 15),
                "14:00",
                EASTERN,
                List.of(),
                2,
                RecurrenceEndMode.COUNT,
                3,
                null);

        List<ExpandedOccurrence> occurrences = expander.expand(request, EASTERN);

        assertThat(occurrences).hasSize(3);
        assertThat(occurrences.get(0).localDate()).isEqualTo(LocalDate.of(2026, 1, 15));
        assertThat(occurrences.get(1).localDate()).isEqualTo(LocalDate.of(2026, 3, 15));
        assertThat(occurrences.get(2).localDate()).isEqualTo(LocalDate.of(2026, 5, 15));
    }

    @Test
    @DisplayName("Should filter monthly recurrence by selected months")
    void expandMonthlyFilteredMonths() {
        RecurrenceRuleRequest request = monthlyRule(
                LocalDate.of(2026, 1, 10),
                "14:00",
                EASTERN,
                List.of(1, 6, 12),
                1,
                RecurrenceEndMode.UNTIL,
                null,
                LocalDate.of(2026, 12, 31));

        List<ExpandedOccurrence> occurrences = expander.expand(request, EASTERN);

        assertThat(occurrences).extracting(ExpandedOccurrence::localDate)
                .containsExactly(
                        LocalDate.of(2026, 1, 10),
                        LocalDate.of(2026, 6, 10),
                        LocalDate.of(2026, 12, 10));
    }

    @Test
    @DisplayName("Should cap expansion at 60 sessions")
    void capAtSixtySessions() {
        RecurrenceRuleRequest request = weeklyRule(
                LocalDate.of(2026, 1, 1),
                "14:00",
                EASTERN,
                List.of(1, 2, 3, 4, 5),
                1,
                RecurrenceEndMode.COUNT,
                100,
                null);

        List<ExpandedOccurrence> occurrences = expander.expand(request, EASTERN);

        assertThat(occurrences).hasSize(60);
    }

    @Test
    @DisplayName("Should reject rules that produce no dates")
    void rejectEmptyExpansion() {
        RecurrenceRuleRequest request = weeklyRule(
                LocalDate.of(2026, 6, 10),
                "14:00",
                EASTERN,
                List.of(1),
                1,
                RecurrenceEndMode.UNTIL,
                null,
                LocalDate.of(2026, 6, 9));

        assertThatThrownBy(() -> expander.expand(request, EASTERN))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Until date cannot be before start date");
    }

    private RecurrenceRuleRequest weeklyRule(
            LocalDate startDate,
            String sessionTime,
            ZoneId timezone,
            List<Integer> daysOfWeek,
            int interval,
            RecurrenceEndMode endMode,
            Integer count,
            LocalDate untilDate) {
        RecurrenceRuleRequest request = baseRule(startDate, sessionTime, timezone);
        request.setRecurrenceType(RecurrenceType.WEEKLY);
        request.setDaysOfWeek(daysOfWeek);
        request.setInterval(interval);
        request.setEndMode(endMode);
        request.setCount(count);
        request.setUntilDate(untilDate);
        return request;
    }

    private RecurrenceRuleRequest monthlyRule(
            LocalDate startDate,
            String sessionTime,
            ZoneId timezone,
            List<Integer> monthsOfYear,
            int interval,
            RecurrenceEndMode endMode,
            Integer count,
            LocalDate untilDate) {
        RecurrenceRuleRequest request = baseRule(startDate, sessionTime, timezone);
        request.setRecurrenceType(RecurrenceType.MONTHLY);
        request.setMonthsOfYear(monthsOfYear);
        request.setInterval(interval);
        request.setEndMode(endMode);
        request.setCount(count);
        request.setUntilDate(untilDate);
        return request;
    }

    private RecurrenceRuleRequest baseRule(LocalDate startDate, String sessionTime, ZoneId timezone) {
        RecurrenceRuleRequest request = new RecurrenceRuleRequest();
        request.setClientId(1L);
        request.setTherapistId(2L);
        request.setServiceId(3L);
        request.setSessionMode("in_person");
        request.setSessionDate(sessionInstant(startDate, sessionTime, timezone));
        request.setTimezone(timezone.getId());
        return request;
    }

    private static Instant sessionInstant(LocalDate date, String sessionTime, ZoneId timezone) {
        return ZonedDateTime.of(date, java.time.LocalTime.parse(sessionTime), timezone).toInstant();
    }
}
