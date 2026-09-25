package com.smart.therapy.flow.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client employment and socioeconomic information")
public class ClientEmploymentResponse {

    @Schema(description = "Employment ID", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Employment status option key", example = "employed_full_time")
    private String employmentStatus;

    @Schema(description = "Employer name", example = "Tech Corp Inc.")
    private String employerName;

    @Schema(description = "Job title", example = "Software Engineer")
    private String jobTitle;

    @Schema(description = "Employment start date", example = "2020-01-15")
    private LocalDate employmentStartDate;

    @Schema(description = "Employment end date", example = "2025-12-31")
    private LocalDate employmentEndDate;

    @Schema(description = "Whether currently employed", example = "true")
    private Boolean isCurrentlyEmployed;

    @Schema(description = "Education level option key", example = "bachelor")
    private String educationLevel;

    @Schema(description = "Field of study", example = "Computer Science")
    private String fieldOfStudy;

    @Schema(description = "School name", example = "University of Toronto")
    private String schoolName;

    @Schema(description = "Graduation year", example = "2018")
    private Integer graduationYear;

    @Schema(description = "Whether currently a student", example = "false")
    private Boolean isStudent;

    @Schema(description = "Occupation category", example = "Technology")
    private String occupationCategory;

    @Schema(description = "Work hours per week", example = "40")
    private Integer workHoursPerWeek;

    @Schema(description = "Whether shift work", example = "false")
    private Boolean shiftWork;

    @Schema(description = "Whether remote work", example = "true")
    private Boolean remoteWork;

    @Schema(description = "Annual income", example = "75000.00")
    private BigDecimal annualIncome;

    @Schema(description = "Household income", example = "100000.00")
    private BigDecimal householdIncome;

    @Schema(description = "Number of dependents", example = "2")
    private Integer dependents;

    @Schema(description = "Household size", example = "4")
    private Integer householdSize;

    @Schema(description = "Whether experiencing financial hardship", example = "false")
    private Boolean financialHardship;

    @Schema(description = "Eligible for sliding scale", example = "false")
    private Boolean eligibleForSlidingScale;

    @Schema(description = "Sliding scale percentage", example = "25.00")
    private BigDecimal slidingScalePercentage;

    @Schema(description = "Disability status", example = "None")
    private String disabilityStatus;

    @Schema(description = "Veteran status", example = "false")
    private Boolean veteranStatus;

    @Schema(description = "Military branch", example = "Army")
    private String militaryBranch;

    @Schema(description = "Military service years", example = "4")
    private Integer militaryServiceYears;

    @Schema(description = "Years of experience", example = "5")
    private Integer yearsOfExperience;

    @Schema(description = "Employment display status", example = "Employed Full Time - Software Engineer at Tech Corp Inc.")
    private String employmentDisplayStatus;

    @Schema(description = "Whether employed", example = "true")
    private Boolean isEmployed;

    @Schema(description = "Whether retired", example = "false")
    private Boolean isRetired;

    @Schema(description = "Whether unemployed", example = "false")
    private Boolean isUnemployed;

    @Schema(description = "Has financial hardship", example = "false")
    private Boolean hasFinancialHardship;

    @Schema(description = "Is veteran", example = "false")
    private Boolean isVeteran;

    @Schema(description = "Has disability", example = "false")
    private Boolean hasDisability;

    @Schema(description = "Notes", example = "Client works from home")
    private String notes;

    @Schema(description = "When this employment info was created")
    private Instant createdAt;

    @Schema(description = "When this employment info was last updated")
    private Instant updatedAt;
}
