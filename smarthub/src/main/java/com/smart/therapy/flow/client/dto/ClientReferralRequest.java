package com.smart.therapy.flow.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create or update client referral information")
public class ClientReferralRequest {

    @PastOrPresent(message = "Referral date must be in the past or present")
    @Schema(description = "Date of referral", example = "2025-01-15")
    private LocalDate referralDate;

    @Size(max = 500, message = "Referral source must not exceed 500 characters")
    @Schema(description = "Source of referral (Self, Physician, Insurance, School, Court, etc.)", example = "Physician")
    private String referralSource;

    @Size(max = 255, message = "Referral type must not exceed 255 characters")
    @Schema(description = "Type of referral (Internal, External, Emergency, Court-Ordered, etc.)", example = "External")
    private String referralType;

    @Size(max = 500, message = "Referrer name must not exceed 500 characters")
    @JsonAlias({"referringPersonName"})
    @Schema(description = "Name of person/organization making the referral (alias: referringPersonName)", example = "Dr. Jane Smith")
    private String referrerName;

    @Size(max = 255, message = "Referrer title must not exceed 255 characters")
    @Schema(description = "Title/credentials of referrer (MD, PhD, LCSW, etc.)", example = "MD")
    private String referrerTitle;

    @Size(max = 500, message = "Referrer organization must not exceed 500 characters")
    @Schema(description = "Organization of referrer", example = "City Hospital")
    private String referrerOrganization;

    @Pattern(regexp = "^[+]?[\\d\\s\\-()]+$", message = "Invalid referrer phone format")
    @Size(max = 20, message = "Referrer phone must not exceed 20 characters")
    @Schema(description = "Referrer phone number", example = "+1-555-123-4567")
    private String referrerPhone;

    @Email(message = "Invalid referrer email format")
    @Size(max = 255, message = "Referrer email must not exceed 255 characters")
    @Schema(description = "Referrer email address", example = "jane.smith@hospital.com")
    private String referrerEmail;

    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @Schema(description = "Reference number from referrer", example = "REF-2025-001")
    private String referenceNumber;

    @Size(max = 500, message = "Client source must not exceed 500 characters")
    @JsonAlias({"legacyReferral"})
    @Schema(description = "How client heard about the service (alias: legacyReferral)", example = "Google Search")
    private String clientSource;

    @Schema(description = "Whether this is a court-ordered referral", example = "false")
    private Boolean isCourtOrdered;

    @Size(max = 255, message = "Court order number must not exceed 255 characters")
    @Schema(description = "Court order number if applicable", example = "CO-2025-123")
    private String courtOrderNumber;

    @Size(max = 255, message = "Court jurisdiction must not exceed 255 characters")
    @Schema(description = "Court jurisdiction if applicable", example = "Superior Court of Ontario")
    private String courtJurisdiction;

    @Schema(description = "Whether reporting is required", example = "false")
    private Boolean requiresReporting;

    @Size(max = 100, message = "Reporting frequency must not exceed 100 characters")
    @Schema(description = "Reporting frequency (Weekly, Monthly, Upon Completion, etc.)", example = "Monthly")
    private String reportingFrequency;

    @Size(max = 500, message = "Reporting recipient must not exceed 500 characters")
    @Schema(description = "Who should receive reports", example = "Court Clerk")
    private String reportingRecipient;

    @Schema(description = "Consent to contact referrer", example = "true")
    private Boolean consentToContactReferrer;

    @Size(max = 255, message = "Marketing campaign must not exceed 255 characters")
    @Schema(description = "Marketing campaign that led to referral", example = "Spring 2025 Campaign")
    private String marketingCampaign;

    @Size(max = 100, message = "Promo code must not exceed 100 characters")
    @Schema(description = "Promotional code used", example = "SPRING2025")
    private String promoCode;

    @Schema(description = "Referral notes", example = "Client referred for anxiety treatment")
    private String referralNotes;

    @Schema(description = "Initial intake summary/presenting problem", example = "Client presents with symptoms of anxiety and depression")
    private String intakeSummary;
}

