package com.smart.therapy.flow.common.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Validates a feature code against core enum and/or catalog.
 */
@Target({ FIELD, PARAMETER })
@Retention(RUNTIME)
@Constraint(validatedBy = ValidFeatureCodeValidator.class)
@Documented
public @interface ValidFeatureCode {

    String message() default "Invalid feature code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    Mode mode() default Mode.CORE_OR_CATALOG;

    enum Mode {
        CORE_ONLY,
        CATALOG_ONLY,
        CORE_OR_CATALOG
    }
}
