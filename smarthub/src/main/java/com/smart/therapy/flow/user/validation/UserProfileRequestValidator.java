package com.smart.therapy.flow.user.validation;

import com.smart.therapy.flow.common.exception.BadRequestException;
import com.smart.therapy.flow.common.service.TimezoneService;
import com.smart.therapy.flow.user.dto.UserProfileRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Validates user profile creation and update requests.
 * 
 * This class encapsulates all validation logic for user profile requests,
 * ensuring business rules are enforced consistently.
 * 
 * Responsibilities:
 * - Required field validation
 * - Business rule enforcement
 * - Data format validation (timezone, session duration, email, etc.)
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class UserProfileRequestValidator {
    private final TimezoneService timezoneService;


    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^[+]?[0-9\\s\\-\\(\\)]{7,20}$"
    );

    /**
     * Validate user profile creation request.
     * 
     * @param request The profile creation request
     * @throws BadRequestException if validation fails
     */
    public void validateCreateRequest(UserProfileRequest request) {
        Objects.requireNonNull(request, "Request is required");
        
        // Validate basic info
        validateEmail(request.getEmail());
        validatePhone(request.getEmergencyContactPhone());
        validateEmergencyContactEmail(request.getEmergencyContactEmail());
        
        // Validate password if provided
        validatePasswordChange(request);
        
        // Validate Zoom credentials if provided
        validateZoomCredentials(request);
        
        // Validate business rules
        validateLicenseNumber(request.getLicenseNumber());
        validateSessionDuration(request.getSessionDuration());
        validateMaxClientsPerDay(request.getMaxClientsPerDay());
        validateTimezone(request.getTimezone());
        validateYearsOfExperience(request.getYearsOfExperience());
    }

    /**
     * Validate user profile update request.
     * 
     * @param request The profile update request
     * @throws BadRequestException if validation fails
     */
    public void validateUpdateRequest(UserProfileRequest request) {
        Objects.requireNonNull(request, "Request is required");
        
        // Validate basic info
        validateEmail(request.getEmail());
        validatePhone(request.getEmergencyContactPhone());
        validateEmergencyContactEmail(request.getEmergencyContactEmail());
        
        // Validate business rules (same as create)
        validateLicenseNumber(request.getLicenseNumber());
        validateSessionDuration(request.getSessionDuration());
        validateMaxClientsPerDay(request.getMaxClientsPerDay());
        validateTimezone(request.getTimezone());
        validateYearsOfExperience(request.getYearsOfExperience());
    }

    /**
     * Validate license number length against database column limit.
     */
    private void validateLicenseNumber(String licenseNumber) {
        if (StringUtils.hasText(licenseNumber) && licenseNumber.length() > 50) {
            throw new BadRequestException("License number cannot exceed 50 characters");
        }
    }

    /**
     * Validate session duration (must be positive and reasonable).
     */
    private void validateSessionDuration(Integer sessionDuration) {
        if (sessionDuration != null) {
            if (sessionDuration <= 0) {
                throw new BadRequestException("Session duration must be greater than 0");
            }
            if (sessionDuration > 480) { // 8 hours max
                throw new BadRequestException("Session duration cannot exceed 480 minutes (8 hours)");
            }
            if (sessionDuration < 15) {
                throw new BadRequestException("Session duration must be at least 15 minutes");
            }
        }
    }

    /**
     * Validate max clients per day (must be positive and reasonable).
     */
    private void validateMaxClientsPerDay(Integer maxClientsPerDay) {
        if (maxClientsPerDay != null) {
            if (maxClientsPerDay <= 0) {
                throw new BadRequestException("Max clients per day must be greater than 0");
            }
            if (maxClientsPerDay > 50) {
                throw new BadRequestException("Max clients per day cannot exceed 50");
            }
        }
    }

    /**
     * Validate timezone (must be valid IANA timezone ID).
     */
    private void validateTimezone(String timezone) {
        if (StringUtils.hasText(timezone)) {
            try {
                timezoneService.normalizeTimezoneId(timezone);
            } catch (Exception e) {
                throw new BadRequestException("Invalid timezone: " + timezone + ". Must be a valid IANA timezone ID (e.g., America/New_York or Asia/Karachi)");
            }
        }
    }

    /**
     * Validate email format.
     */
    private void validateEmail(String email) {
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid email format: " + email);
        }
    }

    /**
     * Validate phone number format.
     */
    private void validatePhone(String phone) {
        if (StringUtils.hasText(phone) && !PHONE_PATTERN.matcher(phone).matches()) {
            throw new BadRequestException("Invalid phone number format: " + phone + ". Must be 7-20 characters with optional +, spaces, dashes, or parentheses");
        }
    }

    /**
     * Validate emergency contact email format.
     */
    private void validateEmergencyContactEmail(String email) {
        if (StringUtils.hasText(email) && !EMAIL_PATTERN.matcher(email).matches()) {
            throw new BadRequestException("Invalid emergency contact email format: " + email);
        }
    }

    /**
     * Validate years of experience (must be reasonable).
     */
    private void validateYearsOfExperience(Integer years) {
        if (years != null) {
            if (years < 0) {
                throw new BadRequestException("Years of experience cannot be negative");
            }
            if (years > 100) {
                throw new BadRequestException("Years of experience cannot exceed 100");
            }
        }
    }

    /**
     * Validate password change request.
     */
    private void validatePasswordChange(UserProfileRequest request) {
        if (request.getNewPassword() != null) {
            if (request.getNewPassword().length() < 6) {
                throw new BadRequestException("New password must be at least 6 characters");
            }
            if (request.getConfirmNewPassword() == null || request.getConfirmNewPassword().isBlank()) {
                throw new BadRequestException("Confirm password is required when changing password");
            }
            if (!request.getNewPassword().equals(request.getConfirmNewPassword())) {
                throw new BadRequestException("New password and confirm password do not match");
            }
        }
    }

    /**
     * Validate Zoom credentials if provided.
     */
    private void validateZoomCredentials(UserProfileRequest request) {
        // If any Zoom field is provided, all required fields must be provided
        boolean hasAnyZoomField = StringUtils.hasText(request.getZoomAccountId()) 
                || StringUtils.hasText(request.getZoomClientId()) 
                || StringUtils.hasText(request.getZoomClientSecret());
        
        if (hasAnyZoomField) {
            if (!StringUtils.hasText(request.getZoomAccountId())) {
                throw new BadRequestException("Zoom Account ID is required when providing Zoom credentials");
            }
            if (!StringUtils.hasText(request.getZoomClientId())) {
                throw new BadRequestException("Zoom Client ID is required when providing Zoom credentials");
            }
            if (!StringUtils.hasText(request.getZoomClientSecret())) {
                throw new BadRequestException("Zoom Client Secret is required when providing Zoom credentials");
            }
        }
    }
}
