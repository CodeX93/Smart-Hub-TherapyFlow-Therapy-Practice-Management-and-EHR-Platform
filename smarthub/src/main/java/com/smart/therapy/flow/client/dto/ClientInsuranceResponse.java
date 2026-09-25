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
@Schema(description = "Client insurance information")
public class ClientInsuranceResponse {

    @Schema(description = "Insurance ID", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Insurance provider option key", example = "blue_cross")
    private String insuranceProvider;

    @Schema(description = "Insurance type option key", example = "private_insurance")
    private String insuranceType;

    @Schema(description = "Policy number", example = "POL123456789")
    private String policyNumber;

    @Schema(description = "Group number", example = "GRP001")
    private String groupNumber;

    @Schema(description = "Subscriber name", example = "John Doe")
    private String subscriberName;

    @Schema(description = "Subscriber relationship", example = "Self")
    private String subscriberRelationship;

    @Schema(description = "Insurance phone", example = "+1-555-123-4567")
    private String insurancePhone;

    @Schema(description = "Insurance email", example = "support@insurance.com")
    private String insuranceEmail;

    @Schema(description = "Copay amount", example = "25.00")
    private BigDecimal copayAmount;

    @Schema(description = "Deductible amount", example = "500.00")
    private BigDecimal deductible;

    @Schema(description = "Deductible met so far", example = "250.00")
    private BigDecimal deductibleMet;

    @Schema(description = "Out of pocket maximum", example = "5000.00")
    private BigDecimal outOfPocketMax;

    @Schema(description = "Coverage percentage", example = "80.00")
    private BigDecimal coveragePercentage;

    @Schema(description = "Effective date", example = "2025-01-01")
    private LocalDate effectiveDate;

    @Schema(description = "Expiry date", example = "2025-12-31")
    private LocalDate expiryDate;

    @Schema(description = "Whether insurance is active", example = "true")
    private Boolean isActive;

    @Schema(description = "Whether insurance is verified", example = "false")
    private Boolean isVerified;

    @Schema(description = "Whether authorization is required", example = "false")
    private Boolean authorizationRequired;

    @Schema(description = "Authorization number", example = "AUTH123456")
    private String authorizationNumber;

    @Schema(description = "Authorization expiration date", example = "2025-06-30")
    private LocalDate authorizationExpiresAt;

    @Schema(description = "Mental health coverage", example = "true")
    private Boolean mentalHealthCoverage;

    @Schema(description = "Telehealth coverage", example = "false")
    private Boolean telehealthCoverage;

    @Schema(description = "Sessions per year", example = "20")
    private Integer sessionsPerYear;

    @Schema(description = "Sessions used", example = "5")
    private Integer sessionsUsed;

    @Schema(description = "Remaining sessions", example = "15")
    private Integer remainingSessions;

    @Schema(description = "Remaining deductible", example = "250.00")
    private BigDecimal remainingDeductible;

    @Schema(description = "Whether deductible is met", example = "false")
    private Boolean isDeductibleMet;

    @Schema(description = "Whether authorization is valid", example = "true")
    private Boolean isAuthorizationValid;

    @Schema(description = "Notes", example = "Requires pre-authorization")
    private String notes;

    @Schema(description = "When this insurance was created")
    private Instant createdAt;

    @Schema(description = "When this insurance was last updated")
    private Instant updatedAt;
}

