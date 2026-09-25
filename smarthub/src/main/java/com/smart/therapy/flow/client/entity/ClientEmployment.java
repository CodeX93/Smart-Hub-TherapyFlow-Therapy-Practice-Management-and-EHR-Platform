package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Normalized employment and socioeconomic information for clients
 * Separates demographic/socioeconomic data from core client profile
 * Benefits:
 * - Socioeconomic data changes independently from client profile
 * - Can track employment history and changes
 * - Optional - not all clients provide this information
 * - Better privacy controls for sensitive information
 */
@Entity
@Table(name = "client_employment", indexes = {
        @Index(name = "idx_client_employment_client", columnList = "client_id"),
        @Index(name = "idx_client_employment_status", columnList = "employment_status"),
        @Index(name = "idx_client_employment_education", columnList = "education_level")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientEmployment extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    @JsonIgnore
    private Client client;

    @Column(name = "employment_status", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String employmentStatus;

    @Column(name = "employer_name")
    @Size(max = 500, message = "Employer name must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String employerName;

    @Column(name = "job_title")
    @Size(max = 255, message = "Job title must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String jobTitle;

    @Column(name = "employment_start_date")
    private LocalDate employmentStartDate;

    @Column(name = "employment_end_date")
    private LocalDate employmentEndDate;

    @Column(name = "is_currently_employed")
    @Builder.Default
    private Boolean isCurrentlyEmployed = false;

    @Column(name = "education_level", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String educationLevel;

    @Column(name = "field_of_study")
    @Size(max = 255, message = "Field of study must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String fieldOfStudy;

    @Column(name = "school_name")
    @Size(max = 500, message = "School name must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String schoolName;

    @Column(name = "graduation_year")
    @Min(value = 1900, message = "Graduation year must be after 1900")
    @Max(value = 2100, message = "Graduation year must be before 2100")
    private Integer graduationYear;

    @Column(name = "is_student")
    @Builder.Default
    private Boolean isStudent = false;

    @Column(name = "occupation_category")
    @Size(max = 255, message = "Occupation category must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String occupationCategory; // e.g., "Healthcare", "Education", "Technology"

    @Column(name = "work_hours_per_week")
    @Min(value = 0, message = "Work hours per week must be non-negative")
    @Max(value = 168, message = "Work hours per week cannot exceed 168")
    private Integer workHoursPerWeek;

    @Column(name = "shift_work")
    @Builder.Default
    private Boolean shiftWork = false;

    @Column(name = "remote_work")
    @Builder.Default
    private Boolean remoteWork = false;

    @Column(name = "annual_income", precision = 12, scale = 2)
    @DecimalMin(value = "0.0", message = "Annual income must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid annual income format")
    private BigDecimal annualIncome;

    @Column(name = "household_income", precision = 12, scale = 2)
    @DecimalMin(value = "0.0", message = "Household income must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid household income format")
    private BigDecimal householdIncome;

    @Column(name = "dependents")
    @Min(value = 0, message = "Dependents must be non-negative")
    private Integer dependents;

    @Column(name = "household_size")
    @Min(value = 1, message = "Household size must be at least 1")
    private Integer householdSize;

    @Column(name = "financial_hardship")
    @Builder.Default
    private Boolean financialHardship = false;

    @Column(name = "eligible_for_sliding_scale")
    @Builder.Default
    private Boolean eligibleForSlidingScale = false;

    @Column(name = "sliding_scale_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Sliding scale percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Sliding scale percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Invalid sliding scale percentage format")
    private BigDecimal slidingScalePercentage;

    @Column(name = "disability_status")
    @Size(max = 100, message = "Disability status must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String disabilityStatus; // e.g., "None", "Physical", "Mental", "Both"

    @Column(name = "veteran_status")
    @Builder.Default
    private Boolean veteranStatus = false;

    @Column(name = "military_branch")
    @Size(max = 100, message = "Military branch must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String militaryBranch;

    @Column(name = "military_service_years")
    @Min(value = 0, message = "Military service years must be non-negative")
    private Integer militaryServiceYears;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    // Helper methods
    public boolean isEmployed() {
        return SystemOptionKeyMatcher.matchesAny(this.employmentStatus,
                "employed_full_time", "employed_part_time", "self_employed");
    }

    public boolean isStudent() {
        return Boolean.TRUE.equals(this.isStudent)
                || SystemOptionKeyMatcher.matchesAny(this.employmentStatus, "student");
    }

    public boolean isRetired() {
        return SystemOptionKeyMatcher.matchesAny(this.employmentStatus, "retired");
    }

    public boolean isUnemployed() {
        return SystemOptionKeyMatcher.matchesAny(this.employmentStatus, "unemployed");
    }

    public boolean hasFinancialHardship() {
        return Boolean.TRUE.equals(this.financialHardship);
    }

    public boolean isVeteran() {
        return Boolean.TRUE.equals(this.veteranStatus);
    }

    public boolean hasDisability() {
        return this.disabilityStatus != null &&
                !this.disabilityStatus.equalsIgnoreCase("None") &&
                !this.disabilityStatus.isBlank();
    }

    public String getEmploymentDisplayStatus() {
        if (this.employmentStatus == null) {
            return "Not Specified";
        }

        StringBuilder sb = new StringBuilder(
                this.employmentStatus != null ? this.employmentStatus.replace('_', ' ') : "Not Specified");

        if (this.isEmployed() && this.jobTitle != null && !this.jobTitle.isBlank()) {
            sb.append(" - ").append(this.jobTitle);
        }

        if (this.isEmployed() && this.employerName != null && !this.employerName.isBlank()) {
            sb.append(" at ").append(this.employerName);
        }

        return sb.toString();
    }

    public Integer getYearsOfExperience() {
        if (this.employmentStartDate == null) {
            return null;
        }

        LocalDate endDate = this.employmentEndDate != null ? this.employmentEndDate : LocalDate.now();
        return java.time.Period.between(this.employmentStartDate, endDate).getYears();
    }
}
