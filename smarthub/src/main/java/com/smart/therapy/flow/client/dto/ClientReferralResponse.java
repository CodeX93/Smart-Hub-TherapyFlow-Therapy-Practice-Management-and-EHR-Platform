package com.smart.therapy.flow.client.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Client referral information")
public class ClientReferralResponse {

    @Schema(description = "Referral ID", example = "1")
    private Long id;

    @Schema(description = "Client ID", example = "1")
    private Long clientId;

    @Schema(description = "Referral date", example = "2025-01-15")
    private LocalDate referralDate;

    @Schema(description = "Referral source", example = "Physician")
    private String referralSource;

    @Schema(description = "Referral type", example = "External")
    private String referralType;

    @Schema(description = "Referrer name", example = "Dr. Jane Smith")
    private String referrerName;

    @Schema(description = "Referrer title", example = "MD")
    private String referrerTitle;

    @Schema(description = "Referrer organization", example = "City Hospital")
    private String referrerOrganization;

    @Schema(description = "Referrer phone", example = "+1-555-123-4567")
    private String referrerPhone;

    @Schema(description = "Referrer email", example = "jane.smith@hospital.com")
    private String referrerEmail;

    @Schema(description = "Reference number", example = "REF-2025-001")
    private String referenceNumber;

    @Schema(description = "Client source", example = "Google Search")
    private String clientSource;

    @Schema(description = "Whether court-ordered", example = "false")
    private Boolean isCourtOrdered;

    @Schema(description = "Court order number", example = "CO-2025-123")
    private String courtOrderNumber;

    @Schema(description = "Court jurisdiction", example = "Superior Court of Ontario")
    private String courtJurisdiction;

    @Schema(description = "Whether reporting required", example = "false")
    private Boolean requiresReporting;

    @Schema(description = "Reporting frequency", example = "Monthly")
    private String reportingFrequency;

    @Schema(description = "Reporting recipient", example = "Court Clerk")
    private String reportingRecipient;

    @Schema(description = "Consent to contact referrer", example = "true")
    private Boolean consentToContactReferrer;

    @Schema(description = "Marketing campaign", example = "Spring 2025 Campaign")
    private String marketingCampaign;

    @Schema(description = "Promo code", example = "SPRING2025")
    private String promoCode;

    @Schema(description = "Referral notes", example = "Client referred for anxiety treatment")
    private String referralNotes;

    @Schema(description = "Intake summary", example = "Client presents with symptoms of anxiety and depression")
    private String intakeSummary;

    @Schema(description = "Referrer display name", example = "Dr. Jane Smith, MD (City Hospital)")
    private String referrerDisplayName;

    @Schema(description = "Referral type display", example = "External")
    private String referralTypeDisplay;

    @Schema(description = "Can contact referrer", example = "true")
    private Boolean canContactReferrer;

    @Schema(description = "When this referral was created")
    private Instant createdAt;

    @Schema(description = "When this referral was last updated")
    private Instant updatedAt;
}

