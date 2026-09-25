package com.smart.therapy.flow.client.dto;

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
@Schema(description = "Request to create or update client insurance information")
public class ClientInsuranceRequest {

    @NotBlank(message = "Insurance provider is required")
    @Schema(description = "Insurance provider option key from insurance_providers catalog", example = "blue_cross")
    private String insuranceProvider;

    @Schema(description = "Insurance type option key from insurance_types catalog", example = "private_insurance")
    private String insuranceType;

    @NotBlank(message = "Policy number is required")
    @Size(max = 255, message = "Policy number must not exceed 255 characters")
    @Schema(description = "Insurance policy number", example = "POL123456789", requiredMode = Schema.RequiredMode.REQUIRED)
    private String policyNumber;

    @Size(max = 255, message = "Group number must not exceed 255 characters")
    @Schema(description = "Insurance group number", example = "GRP001")
    private String groupNumber;

    @Size(max = 255, message = "Subscriber name must not exceed 255 characters")
    @Schema(description = "Name of policy subscriber", example = "John Doe")
    private String subscriberName;

    @Size(max = 50, message = "Subscriber relationship must not exceed 50 characters")
    @Schema(description = "Relationship to subscriber (Self, Spouse, Parent, Child, Other)", example = "Self")
    private String subscriberRelationship;

    @Pattern(regexp = "^[+]?[\\d\\s\\-()]+$", message = "Invalid insurance phone format")
    @Size(max = 20, message = "Insurance phone must not exceed 20 characters")
    @Schema(description = "Insurance company phone number", example = "+1-555-123-4567")
    private String insurancePhone;

    @Email(message = "Invalid insurance email format")
    @Size(max = 255, message = "Insurance email must not exceed 255 characters")
    @Schema(description = "Insurance company email", example = "support@insurance.com")
    private String insuranceEmail;

    @DecimalMin(value = "0.0", message = "Copay amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid copay amount format")
    @Schema(description = "Copay amount per session", example = "25.00")
    private BigDecimal copayAmount;

    @DecimalMin(value = "0.0", message = "Deductible must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid deductible format")
    @Schema(description = "Annual deductible amount", example = "500.00")
    private BigDecimal deductible;

    @DecimalMin(value = "0.0", message = "Out of pocket max must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid out of pocket max format")
    @Schema(description = "Maximum out-of-pocket expense", example = "5000.00")
    private BigDecimal outOfPocketMax;

    @DecimalMin(value = "0.0", message = "Coverage percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Coverage percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Invalid coverage percentage format")
    @Schema(description = "Coverage percentage (e.g., 80.00 for 80%)", example = "80.00")
    private BigDecimal coveragePercentage;

    @Schema(description = "Policy effective date", example = "2025-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "Policy expiry date", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(description = "Whether this insurance is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether this insurance has been verified", example = "false")
    private Boolean isVerified;

    @Schema(description = "Whether authorization is required", example = "false")
    private Boolean authorizationRequired;

    @Size(max = 255, message = "Authorization number must not exceed 255 characters")
    @Schema(description = "Authorization number if required", example = "AUTH123456")
    private String authorizationNumber;

    @Schema(description = "Authorization expiration date", example = "2025-06-30")
    private LocalDate authorizationExpiresAt;

    @Schema(description = "Whether mental health is covered", example = "true")
    private Boolean mentalHealthCoverage;

    @Schema(description = "Whether telehealth is covered", example = "false")
    private Boolean telehealthCoverage;

    @Min(value = 0, message = "Sessions per year must be non-negative")
    @Schema(description = "Number of sessions covered per year (null = unlimited)", example = "20")
    private Integer sessionsPerYear;

    @Schema(description = "Additional notes about the insurance", example = "Requires pre-authorization for sessions")
    private String notes;
}

