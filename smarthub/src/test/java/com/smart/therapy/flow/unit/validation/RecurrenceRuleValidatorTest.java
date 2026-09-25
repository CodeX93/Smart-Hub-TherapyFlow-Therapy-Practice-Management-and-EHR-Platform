package com.smart.therapy.flow.unit.validation;

import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import com.smart.therapy.flow.session.enums.SessionType;
import com.smart.therapy.flow.session.validation.RecurrenceRuleValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecurrenceRuleValidator Unit Tests")
class RecurrenceRuleValidatorTest {

    @Mock private ConstraintValidatorContext context;
    @Mock private ConstraintValidatorContext.ConstraintViolationBuilder builder;
    @Mock private ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext nodeBuilder;

    private final RecurrenceRuleValidator validator = new RecurrenceRuleValidator();

    @BeforeEach
    void setUp() {
        lenient().when(context.buildConstraintViolationWithTemplate(anyString())).thenReturn(builder);
        lenient().when(builder.addPropertyNode(anyString())).thenReturn(nodeBuilder);
        lenient().when(nodeBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    @DisplayName("Should accept valid weekly count rule")
    void shouldAcceptValidWeeklyCountRule() {
        assertThat(validator.isValid(validWeeklyCountRule(), context)).isTrue();
    }

    @Test
    @DisplayName("Should reject count mode without count")
    void shouldRejectCountModeWithoutCount() {
        RecurrenceRuleRequest request = validWeeklyCountRule();
        request.setCount(null);
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject until mode without until date")
    void shouldRejectUntilModeWithoutUntilDate() {
        RecurrenceRuleRequest request = validWeeklyCountRule();
        request.setEndMode(RecurrenceEndMode.UNTIL);
        request.setCount(null);
        request.setUntilDate(null);
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject weekly rule without days of week")
    void shouldRejectWeeklyWithoutDays() {
        RecurrenceRuleRequest request = validWeeklyCountRule();
        request.setDaysOfWeek(List.of());
        assertThat(validator.isValid(request, context)).isFalse();
    }

    @Test
    @DisplayName("Should reject monthly rule with days of week")
    void shouldRejectMonthlyWithDaysOfWeek() {
        RecurrenceRuleRequest request = validWeeklyCountRule();
        request.setRecurrenceType(RecurrenceType.MONTHLY);
        request.setDaysOfWeek(List.of(1));
        assertThat(validator.isValid(request, context)).isFalse();
    }

    private RecurrenceRuleRequest validWeeklyCountRule() {
        LocalDate startDate = LocalDate.now(ZoneId.of("America/New_York")).plusWeeks(2);
        while (startDate.getDayOfWeek().getValue() % 7 != 1) {
            startDate = startDate.plusDays(1);
        }

        RecurrenceRuleRequest request = new RecurrenceRuleRequest();
        request.setClientId(1L);
        request.setTherapistId(2L);
        request.setServiceId(3L);
        request.setSessionMode("in_person");
        request.setSessionDate(ZonedDateTime.of(
                startDate,
                java.time.LocalTime.of(14, 0),
                ZoneId.of("America/New_York")).toInstant());
        request.setTimezone("America/New_York");
        request.setRecurrenceType(RecurrenceType.WEEKLY);
        request.setDaysOfWeek(List.of(1, 3));
        request.setInterval(1);
        request.setEndMode(RecurrenceEndMode.COUNT);
        request.setCount(4);
        return request;
    }
}
