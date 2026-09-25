package com.smart.therapy.flow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {

    private Long id;

    // ========================================
    // TAB 1: Basic Info
    // ========================================
    private String fullName;
    private String email;
    private String profilePicture;

    // Emergency Contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactEmail;
    private String emergencyContactRelationship;

    // ========================================
    // TAB 2: License
    // ========================================
    private String licenseNumber;
    private String licenseType;
    private String licenseState;
    private LocalDate licenseExpiry;
    private LicenseStatus licenseStatus;

    // ========================================
    // TAB 3: Specialization
    // ========================================
    private List<String> specializations;
    private List<String> languages;

    // ========================================
    // TAB 4: Background
    // ========================================
    private Integer yearsOfExperience;
    private String clinicalExperience;
    private String researchBackground;
    private List<UserProfileEducationResponse> education;
    private String supervisoryExperience;
    private String careerObjectives;

    // ========================================
    // TAB 5: Schedule
    // ========================================
    private List<String> workingDays;
    private String workingHours;
    /** Consultation-only schedule hours (service-scoped). */
    private String consultationWorkingHours;
    private Integer maxClientsPerDay;
    private Integer sessionDuration;
    private AvailabilityStatus availabilityStatus;
    private String timezone; // IANA timezone ID (e.g., "America/New_York", "Asia/Karachi")

    // Room Configuration
    private Long virtualRoomId;
    private List<Long> availablePhysicalRoomIds;

    // ========================================
    // TAB 6: Zoom Integration
    // ========================================
    private Boolean zoomConfigured;
    private String zoomAccountId;
    private java.time.Instant zoomLastUpdatedAt;

    // ========================================
    // TAB 7: Password
    // ========================================
    private Boolean passwordChangeRequired;
}
