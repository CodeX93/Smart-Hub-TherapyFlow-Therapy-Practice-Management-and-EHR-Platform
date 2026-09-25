package com.smart.therapy.flow.session.validation;

import com.smart.therapy.flow.session.dto.RecurrenceRuleRequest;
import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.util.CollectionUtils;

public class RecurrenceRuleValidator implements ConstraintValidator<ValidRecurrenceRule, RecurrenceRuleRequest> {

    @Override
    public boolean isValid(RecurrenceRuleRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        context.disableDefaultConstraintViolation();
        boolean valid = true;

        if (request.getEndMode() == RecurrenceEndMode.COUNT) {
            if (request.getCount() == null) {
                addViolation(context, "count", "Count is required when endMode is count");
                valid = false;
            }
            if (request.getUntilDate() != null) {
                addViolation(context, "untilDate", "untilDate must not be set when endMode is count");
                valid = false;
            }
        } else if (request.getEndMode() == RecurrenceEndMode.UNTIL) {
            if (request.getUntilDate() == null) {
                addViolation(context, "untilDate", "Until date is required when endMode is until");
                valid = false;
            }
            if (request.getCount() != null) {
                addViolation(context, "count", "count must not be set when endMode is until");
                valid = false;
            }
        }

        if (request.getRecurrenceType() == RecurrenceType.WEEKLY) {
            if (CollectionUtils.isEmpty(request.getDaysOfWeek())) {
                addViolation(context, "daysOfWeek", "At least one day of week is required for weekly recurrence");
                valid = false;
            }
        } else if (request.getRecurrenceType() == RecurrenceType.MONTHLY) {
            if (!CollectionUtils.isEmpty(request.getDaysOfWeek())) {
                addViolation(context, "daysOfWeek", "daysOfWeek is only used for weekly recurrence");
                valid = false;
            }
        }

        return valid;
    }

    private void addViolation(ConstraintValidatorContext context, String field, String message) {
        context.buildConstraintViolationWithTemplate(message)
                .addPropertyNode(field)
                .addConstraintViolation();
    }
}
