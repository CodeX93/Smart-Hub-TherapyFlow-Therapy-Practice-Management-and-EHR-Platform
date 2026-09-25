package com.smart.therapy.flow.session.service;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
public class RecurrenceDateExpander {

    public static final int MAX_SESSIONS = 60;
    public static final int MAX_DAYS = 730;
    private static final DateTimeFormatter LOCAL_DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    public record ExpandedOccurrence(LocalDate localDate, Instant utcDate, String sessionTime) {}

    public List<ExpandedOccurrence> expand(RecurrenceRuleRequest rule, ZoneId timezone) {
        Objects.requireNonNull(timezone, "Timezone is required");
        validateRuleProducesDates(rule);

        ZonedDateTime anchorLocal = rule.getSessionDate().atZone(timezone);
        LocalDate startDate = anchorLocal.toLocalDate();
        LocalTime sessionTime = anchorLocal.toLocalTime();
        int interval = rule.getInterval() != null ? rule.getInterval() : 1;
        int targetCount = rule.getEndMode() == RecurrenceEndMode.COUNT ? rule.getCount() : Integer.MAX_VALUE;
        LocalDate untilDate = rule.getEndMode() == RecurrenceEndMode.UNTIL ? rule.getUntilDate() : null;

        if (untilDate != null && untilDate.isBefore(startDate)) {
            throw new BadRequestException("Until date cannot be before start date");
        }

        List<ExpandedOccurrence> results = new ArrayList<>();

        for (int dayOffset = 0; dayOffset <= MAX_DAYS && results.size() < targetCount && results.size() < MAX_SESSIONS; dayOffset++) {
            LocalDate current = startDate.plusDays(dayOffset);
            if (untilDate != null && current.isAfter(untilDate)) {
                break;
            }

            if (!matchesRecurrencePattern(rule, startDate, current, interval)) {
                continue;
            }

            ZonedDateTime localDateTime = current.atTime(sessionTime).atZone(timezone);
            results.add(new ExpandedOccurrence(
                    current,
                    localDateTime.toInstant(),
                    sessionTime.format(TIME_FORMAT)));
        }

        if (results.isEmpty()) {
            throw new BadRequestException("Recurrence rule produced no session dates");
        }

        return results;
    }

    private void validateRuleProducesDates(RecurrenceRuleRequest rule) {
        if (rule.getSessionDate() == null) {
            throw new BadRequestException("Session date is required");
        }
        if (rule.getRecurrenceType() == RecurrenceType.WEEKLY && CollectionUtils.isEmpty(rule.getDaysOfWeek())) {
            throw new BadRequestException("At least one day of week is required for weekly recurrence");
        }
    }

    private boolean matchesRecurrencePattern(
            RecurrenceRuleRequest rule,
            LocalDate startDate,
            LocalDate current,
            int interval) {
        if (current.isBefore(startDate)) {
            return false;
        }

        if (rule.getRecurrenceType() == RecurrenceType.WEEKLY) {
            return matchesWeekly(rule, startDate, current, interval);
        }
        return matchesMonthly(rule, startDate, current, interval);
    }

    private boolean matchesWeekly(RecurrenceRuleRequest rule, LocalDate startDate, LocalDate current, int interval) {
        Set<Integer> days = new HashSet<>(rule.getDaysOfWeek());
        int dayOfWeek = current.getDayOfWeek().getValue() % 7;
        if (!days.contains(dayOfWeek)) {
            return false;
        }
        long daysBetween = ChronoUnit.DAYS.between(startDate, current);
        int weekIndex = (int) (daysBetween / 7);
        return weekIndex % interval == 0;
    }

    private boolean matchesMonthly(RecurrenceRuleRequest rule, LocalDate startDate, LocalDate current, int interval) {
        if (!CollectionUtils.isEmpty(rule.getMonthsOfYear())
                && !rule.getMonthsOfYear().contains(current.getMonthValue())) {
            return false;
        }

        int targetDay = Math.min(startDate.getDayOfMonth(), current.lengthOfMonth());
        if (current.getDayOfMonth() != targetDay) {
            return false;
        }

        YearMonth startMonth = YearMonth.from(startDate);
        YearMonth currentMonth = YearMonth.from(current);
        long monthsBetween = ChronoUnit.MONTHS.between(startMonth, currentMonth);
        return monthsBetween % interval == 0;
    }

    public String formatLocalDate(LocalDate date) {
        return date.format(LOCAL_DATE_FORMAT);
    }
}
