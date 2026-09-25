package com.smart.therapy.flow.billing.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = PaymentDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PaymentDateConstraint {
    String message() default "Payment date cannot be in the future";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
