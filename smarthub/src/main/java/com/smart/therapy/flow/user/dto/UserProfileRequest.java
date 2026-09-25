package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Schema(description = "Request to create or update a user profile")
public class UserProfileRequest {

    // ========================================
    // TAB 1: Basic Info
    // ========================================
    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    @Schema(description = "User's full name", example = "Dr. John Doe", maxLength = 150)
    private String fullName;

    @Email(message = "Invalid email format")
    @Size(max = 150, message = "Email cannot exceed 150 characters")
    @Schema(description = "User's email address", example = "john.doe@therapyflow.pro", maxLength = 150)
    private String email;

    // ========================================
    // TAB 2: License
    // ========================================
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    @Schema(description = "Professional license number", example = "LMFT-12345", maxLength = 50)
    private String licenseNumber;

    @Size(max = 100, message = "License type cannot exceed 100 characters")
    @Schema(description = "License type (e.g., LMFT, LCSW, LPC, PsyD)", example = "LMFT", maxLength = 100)
    private String licenseType;

    @Size(max = 50, message = "License state cannot exceed 50 characters")
    @Schema(description = "State where license is issued", example = "California", maxLength = 50)
    private String licenseState;

    @Schema(description = "License expiration date", example = "2025-12-31", type = "string", format = "date")
    private LocalDate licenseExpiry;

    @Schema(description = "License status", example = "ACTIVE", allowableValues = {"ACTIVE", "INACTIVE"})
    private LicenseStatus licenseStatus;

    // ========================================
    // TAB 3: Specialization
    // ========================================
    @Schema(description = "List of specializations", example = "[\"Anxiety\", \"Depression\", \"Trauma\"]")
    private List<String> specializations;

    @Schema(description = "List of languages spoken", example = "[\"English\", \"Spanish\"]")
    private List<String> languages;

    // ========================================
    // TAB 4: Background
    // ========================================
    @Min(value = 0, message = "Years of experience cannot be negative")
    @Max(value = 100, message = "Years of experience cannot exceed 100")
    @Schema(description = "Years of professional experience", example = "10", minimum = "0", maximum = "100")
    private Integer yearsOfExperience;

    @Schema(description = "Clinical experience summary", example = "Extensive experience in trauma therapy...")
    private String clinicalExperience;

    @Schema(description = "Research background and involvement", example = "Published research on CBT effectiveness...")
    private String researchBackground;

    @Schema(description = "List of education entries")
    private List<UserProfileEducationRequest> education;

    @Schema(description = "Supervisory experience", example = "Supervised 15+ therapists over 5 years...")
    private String supervisoryExperience;

    @Schema(description = "Career objectives and goals", example = "Continue specializing in trauma therapy...")
    private String careerObjectives;

    // ========================================
    // TAB 5: Schedule
    // ========================================
    @Schema(description = "List of working days", example = "[\"monday\", \"tuesday\", \"wednesday\"]")
    private List<String> workingDays; // e.g., ["monday", "tuesday", "wednesday"]
    
    @Schema(description = "JSON string of working hours", 
            example = "[{\"day\":\"monday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"17:00\"}]")
    private String workingHours; // JSON string of time ranges (All-Services schedule)

    @Schema(description = "JSON string of Consultation-only working hours",
            example = "[{\"day\":\"monday\",\"enabled\":true,\"start\":\"09:00\",\"end\":\"12:00\"}]")
    private String consultationWorkingHours;
    
    @Min(value = 1, message = "Max clients per day must be at least 1")
    @Max(value = 50, message = "Max clients per day cannot exceed 50")
    @Schema(description = "Maximum number of clients per day", example = "10", minimum = "1", maximum = "50")
    private Integer maxClientsPerDay;
    
    @Min(value = 15, message = "Session duration must be at least 15 minutes")
    @Max(value = 480, message = "Session duration cannot exceed 480 minutes (8 hours)")
    @Schema(description = "Session duration in minutes (default: 50)", example = "50", minimum = "15", maximum = "480")
    private Integer sessionDuration; // Minutes (default: 50)
    
    @Schema(description = "Availability status", example = "AVAILABLE", 
            allowableValues = {"AVAILABLE", "BUSY", "UNAVAILABLE", "ON_LEAVE"})
    private AvailabilityStatus availabilityStatus;
    
    @Size(max = 50, message = "Timezone cannot exceed 50 characters")
    @Schema(description = "IANA timezone ID", example = "America/New_York", maxLength = 50)
    private String timezone; // IANA timezone ID, e.g., "America/New_York", "Asia/Karachi"

    // Room Configuration
    @Schema(description = "Virtual room ID for online sessions", example = "1")
    private Long virtualRoomId;

    @Schema(description = "List of available physical room IDs for in-person sessions", example = "[1, 2, 3]")
    private List<Long> availablePhysicalRoomIds;

    // ========================================
    // Emergency Contact (Basic Info Tab)
    // ========================================
    @Size(max = 255, message = "Emergency contact name cannot exceed 255 characters")
    @Schema(description = "Emergency contact name", example = "Jane Doe", maxLength = 255)
    private String emergencyContactName;

    @Size(max = 20, message = "Emergency contact phone cannot exceed 20 characters")
    @Schema(description = "Emergency contact phone number", example = "+1-555-123-4567", maxLength = 20)
    private String emergencyContactPhone;

    @Email(message = "Invalid emergency contact email format")
    @Size(max = 255, message = "Emergency contact email cannot exceed 255 characters")
    @Schema(description = "Emergency contact email (optional)", example = "jane.doe@example.com", maxLength = 255)
    private String emergencyContactEmail;

    @Size(max = 100, message = "Emergency contact relationship cannot exceed 100 characters")
    @Schema(description = "Relationship to emergency contact", example = "Spouse", maxLength = 100)
    private String emergencyContactRelationship;

    // ========================================
    // TAB 6: Zoom Integration
    // ========================================
    @Schema(description = "Zoom Account ID", example = "abc123xyz")
    private String zoomAccountId;

    @Schema(description = "Zoom Client ID", example = "your_client_id")
    private String zoomClientId;

    @Schema(description = "Zoom Client Secret", example = "your_client_secret")
    private String zoomClientSecret;

    // ========================================
    // TAB 7: Password
    // ========================================
    @Schema(description = "Current password (required for password change)", example = "CurrentPassword123!")
    private String currentPassword;

    @Size(min = 6, max = 255, message = "New password must be at least 6 characters")
    @Schema(description = "New password (minimum 6 characters)", example = "NewSecurePassword123!", minLength = 6)
    private String newPassword;

    @Size(min = 6, max = 255, message = "Confirm password must be at least 6 characters")
    @Schema(description = "Confirm new password (must match new password)", example = "NewSecurePassword123!", minLength = 6)
    private String confirmNewPassword;
}

