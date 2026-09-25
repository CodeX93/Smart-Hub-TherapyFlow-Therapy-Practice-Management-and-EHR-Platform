package com.smart.therapy.flow.billing.validation;

import com.smart.therapy.flow.common.service.TimezoneService;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/**
 * Payment dates arrive as instants but represent a practice-local calendar day.
 * Validation therefore uses the Administration practice timezone, never the server timezone.
 */
public class PaymentDateValidator implements ConstraintValidator<PaymentDateConstraint, Instant> {

    private TimezoneService timezoneService;

    public PaymentDateValidator() {
        // Required by Bean Validation and retained for isolated unit construction.
    }

    @Autowired
    public PaymentDateValidator(TimezoneService timezoneService) {
        this.timezoneService = timezoneService;
    }

    @Override
    public void initialize(PaymentDateConstraint constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(Instant paymentDate, ConstraintValidatorContext context) {
        // Null is valid - let @NotNull handle required validation
        if (paymentDate == null) {
            return true;
        }

        ZoneId practiceZone = timezoneService != null
                ? timezoneService.getPracticeTimezone()
                : TimezoneService.DEFAULT_PRACTICE_ZONE;
        LocalDate paymentDay = paymentDate.atZone(practiceZone).toLocalDate();
        LocalDate today = LocalDate.now(practiceZone);

        if (paymentDay.isAfter(today)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Payment date cannot be in the future")
                    .addConstraintViolation();
            return false;
        }

        LocalDate twoYearsAgo = today.minusYears(2);
        if (paymentDay.isBefore(twoYearsAgo)) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Payment date cannot be more than 2 years in the past")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}
