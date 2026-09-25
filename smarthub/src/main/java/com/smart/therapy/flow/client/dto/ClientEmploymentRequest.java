package com.smart.therapy.flow.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update client employment and socioeconomic information")
public class ClientEmploymentRequest {

    @Schema(description = "Employment status", example = "EMPLOYED_FULL_TIME")
    private String employmentStatus;

    @Size(max = 500, message = "Employer name must not exceed 500 characters")
    @Schema(description = "Employer name", example = "Tech Corp Inc.")
    private String employerName;

    @Size(max = 255, message = "Job title must not exceed 255 characters")
    @Schema(description = "Job title", example = "Software Engineer")
    private String jobTitle;

    @Schema(description = "Employment start date", example = "2020-01-15")
    private LocalDate employmentStartDate;

    @Schema(description = "Employment end date", example = "2025-12-31")
    private LocalDate employmentEndDate;

    @Schema(description = "Whether currently employed", example = "true")
    private Boolean isCurrentlyEmployed;

    @Schema(description = "Education level", example = "BACHELOR")
    private String educationLevel;

    @Size(max = 255, message = "Field of study must not exceed 255 characters")
    @Schema(description = "Field of study", example = "Computer Science")
    private String fieldOfStudy;

    @Size(max = 500, message = "School name must not exceed 500 characters")
    @Schema(description = "School name", example = "University of Toronto")
    private String schoolName;

    @Min(value = 1900, message = "Graduation year must be after 1900")
    @Max(value = 2100, message = "Graduation year must be before 2100")
    @Schema(description = "Graduation year", example = "2018")
    private Integer graduationYear;

    @Schema(description = "Whether currently a student", example = "false")
    private Boolean isStudent;

    @Size(max = 255, message = "Occupation category must not exceed 255 characters")
    @Schema(description = "Occupation category", example = "Technology")
    private String occupationCategory;

    @Min(value = 0, message = "Work hours per week must be non-negative")
    @Max(value = 168, message = "Work hours per week cannot exceed 168")
    @Schema(description = "Work hours per week", example = "40")
    private Integer workHoursPerWeek;

    @Schema(description = "Whether shift work", example = "false")
    private Boolean shiftWork;

    @Schema(description = "Whether remote work", example = "true")
    private Boolean remoteWork;

    @DecimalMin(value = "0.0", message = "Annual income must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid annual income format")
    @Schema(description = "Annual income", example = "75000.00")
    private BigDecimal annualIncome;

    @DecimalMin(value = "0.0", message = "Household income must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Invalid household income format")
    @Schema(description = "Household income", example = "100000.00")
    private BigDecimal householdIncome;

    @Min(value = 0, message = "Dependents must be non-negative")
    @JsonAlias({"numberOfDependents"})
    @Schema(description = "Number of dependents (alias: numberOfDependents)", example = "2")
    private Integer dependents;

    @Min(value = 1, message = "Household size must be at least 1")
    @Schema(description = "Household size", example = "4")
    private Integer householdSize;

    @Schema(description = "Whether experiencing financial hardship", example = "false")
    private Boolean financialHardship;

    @Schema(description = "Eligible for sliding scale", example = "false")
    private Boolean eligibleForSlidingScale;

    @DecimalMin(value = "0.0", message = "Sliding scale percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Sliding scale percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Invalid sliding scale percentage format")
    @Schema(description = "Sliding scale percentage", example = "25.00")
    private BigDecimal slidingScalePercentage;

    @Size(max = 100, message = "Disability status must not exceed 100 characters")
    @Schema(description = "Disability status (None, Physical, Mental, Both)", example = "None")
    private String disabilityStatus;

    @Schema(description = "Veteran status", example = "false")
    private Boolean veteranStatus;

    @Size(max = 100, message = "Military branch must not exceed 100 characters")
    @Schema(description = "Military branch if veteran", example = "Army")
    private String militaryBranch;

    @Min(value = 0, message = "Military service years must be non-negative")
    @Schema(description = "Years of military service", example = "4")
    private Integer militaryServiceYears;

    @Schema(description = "Additional notes", example = "Client works from home")
    private String notes;
}
