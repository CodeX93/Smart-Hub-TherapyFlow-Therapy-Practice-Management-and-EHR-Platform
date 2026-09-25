package com.smart.therapy.flow.common.validation;

import com.smart.therapy.flow.subscription.feature.CoreFeature;
import com.smart.therapy.flow.subscription.repository.AppFeatureRepository;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@RequiredArgsConstructor
public class ValidFeatureCodeValidator implements ConstraintValidator<ValidFeatureCode, String> {

    private final AppFeatureRepository appFeatureRepository;
    private ValidFeatureCode.Mode mode;

    @Override
    public void initialize(ValidFeatureCode constraintAnnotation) {
        this.mode = constraintAnnotation.mode();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        String normalized = value.trim();
        boolean isCore = CoreFeature.isCoreCode(normalized.toUpperCase(Locale.ROOT));
        boolean inCatalog = appFeatureRepository.existsByCodeIgnoreCase(normalized);

        return switch (mode) {
            case CORE_ONLY -> isCore;
            case CATALOG_ONLY -> inCatalog && !isCore;
            case CORE_OR_CATALOG -> isCore || inCatalog;
        };
    }
}
