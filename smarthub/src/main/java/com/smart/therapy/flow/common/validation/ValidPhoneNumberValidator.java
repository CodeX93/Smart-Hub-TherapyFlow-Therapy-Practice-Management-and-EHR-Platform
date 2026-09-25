package com.smart.therapy.flow.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.regex.Pattern;

public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, String> {

    // Supports formats: (123) 456-7890, 123-456-7890, 123.456.7890, 1234567890, +1 123 456 7890
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[\\+]?[(]?[0-9]{3}[)]?[-\\s\\.]?[0-9]{3}[-\\s\\.]?[0-9]{4,6}$"
    );

    @Override
    public void initialize(ValidPhoneNumber constraintAnnotation) {
        // No initialization needed
    }

    @Override
    public boolean isValid(String phone, ConstraintValidatorContext context) {
        if (phone == null || phone.isEmpty()) {
            return true; // Let @NotNull or @NotBlank handle null/empty validation
        }

        String cleaned = phone.replaceAll("[\\s\\-\\(\\)\\.]", "");
        boolean isValid = PHONE_PATTERN.matcher(phone).matches() || 
                         (cleaned.length() >= 10 && cleaned.length() <= 15 && cleaned.matches("^\\+?[0-9]+$"));

        if (!isValid) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("Phone number must be in format: (123) 456-7890 or 123-456-7890")
                    .addConstraintViolation();
        }

        return isValid;
    }
}

