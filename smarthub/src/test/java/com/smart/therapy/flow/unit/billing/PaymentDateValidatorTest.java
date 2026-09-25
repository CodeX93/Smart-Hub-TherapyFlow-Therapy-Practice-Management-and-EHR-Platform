package com.smart.therapy.flow.unit.billing;

import com.smart.therapy.flow.billing.validation.PaymentDateValidator;
import com.smart.therapy.flow.common.service.TimezoneService;
import jakarta.validation.ConstraintValidatorContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentDateValidatorTest {

    private PaymentDateValidator validator;

    @Mock
    private ConstraintValidatorContext context;

    @Mock
    private ConstraintValidatorContext.ConstraintViolationBuilder violationBuilder;

    @Mock
    private TimezoneService timezoneService;

    @BeforeEach
    void setUp() {
        validator = new PaymentDateValidator(timezoneService);
        lenient().when(timezoneService.getPracticeTimezone()).thenReturn(ZoneOffset.UTC);
        lenient().when(context.buildConstraintViolationWithTemplate(anyString()))
                .thenReturn(violationBuilder);
        lenient().when(violationBuilder.addConstraintViolation()).thenReturn(context);
    }

    @Test
    void acceptsNull() {
        assertThat(validator.isValid(null, context)).isTrue();
    }

    @Test
    void acceptsSameUtcDayEvenWhenInstantIsAfterNow() {
        Instant laterToday = Instant.now().plusSeconds(3_600);
        // Only assert when +1h stays on the same UTC calendar day (avoids midnight edge).
        if (!laterToday.atZone(ZoneOffset.UTC).toLocalDate().equals(LocalDate.now(ZoneOffset.UTC))) {
            laterToday = LocalDate.now(ZoneOffset.UTC).atTime(23, 59, 59).toInstant(ZoneOffset.UTC);
        }
        assertThat(validator.isValid(laterToday, context)).isTrue();
    }

    @Test
    void acceptsUtcMidnightToday() {
        Instant utcMidnight = LocalDate.now(ZoneOffset.UTC).atStartOfDay(ZoneOffset.UTC).toInstant();
        assertThat(validator.isValid(utcMidnight, context)).isTrue();
    }

    @Test
    void rejectsTomorrow() {
        Instant tomorrow = LocalDate.now(ZoneOffset.UTC).plusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        assertThat(validator.isValid(tomorrow, context)).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    void rejectsMoreThanTwoYearsAgo() {
        Instant tooOld = LocalDate.now(ZoneOffset.UTC).minusYears(2).minusDays(1)
                .atStartOfDay(ZoneOffset.UTC)
                .toInstant();
        assertThat(validator.isValid(tooOld, context)).isFalse();
        verify(context).disableDefaultConstraintViolation();
    }

    @Test
    void acceptsPracticeTodayAcrossUtcDateBoundary() {
        ZoneId practiceZone = ZoneId.of("Pacific/Kiritimati");
        when(timezoneService.getPracticeTimezone()).thenReturn(practiceZone);
        Instant practiceMidnight = LocalDate.now(practiceZone).atStartOfDay(practiceZone).toInstant();

        assertThat(validator.isValid(practiceMidnight, context)).isTrue();
    }
}
