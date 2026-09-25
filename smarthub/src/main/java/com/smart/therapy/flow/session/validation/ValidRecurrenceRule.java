package com.smart.therapy.flow.session.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = RecurrenceRuleValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidRecurrenceRule {
    String message() default "Invalid recurrence rule";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
