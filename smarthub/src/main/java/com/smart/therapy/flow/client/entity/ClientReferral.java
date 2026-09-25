package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.client.enums.ReferralSource;
import com.smart.therapy.flow.client.enums.ReferralType;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Normalized referral information for clients
 * Separates one-time referral data from core client profile
 * Benefits:
 * - Referral data is static and doesn't change frequently
 * - Can track detailed referral information without cluttering client profile
 * - Can easily query and report on referral sources
 * - Optional - not all clients have referrals
 */
@Entity
@Table(name = "client_referrals", indexes = {
        @Index(name = "idx_client_referral_client", columnList = "client_id"),
        @Index(name = "idx_client_referral_date", columnList = "referral_date"),
        @Index(name = "idx_client_referral_source", columnList = "referral_source"),
        @Index(name = "idx_client_referral_type", columnList = "referral_type")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class ClientReferral extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    @JsonIgnore
    private Client client;

    @EqualsAndHashCode.Include
    public Long getEntityIdForEquality() {
        return getId();
    }

    @Column(name = "referral_date")
    @PastOrPresent(message = "Referral date must be in the past or present")
    private LocalDate referralDate;

    @Transient
    @PastOrPresent(message = "Start date must be in the past or present")
    private LocalDate startDate;

    @Column(name = "referral_source", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String referralSource; // e.g., "Self", "Physician", "Insurance", "School", "Court"

    @Enumerated(EnumType.STRING)
    @Column(name = "referral_type")
    private ReferralType referralType; // e.g., "Internal", "External", "Emergency", "Court-Ordered"

    @Column(name = "referrer_name", columnDefinition = "TEXT")
    @Size(max = 500, message = "Referrer name must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referrerName;

    @Column(name = "referrer_title")
    @Size(max = 255, message = "Referrer title must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referrerTitle; // e.g., "MD", "PhD", "LCSW"

    @Column(name = "referrer_organization")
    @Size(max = 500, message = "Referrer organization must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referrerOrganization;

    @Column(name = "referrer_phone", length = 20)
    @Pattern(regexp = "^[+]?[\\d\\s\\-()]+$", message = "Invalid referrer phone format")
    @Size(max = 20, message = "Referrer phone must not exceed 20 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referrerPhone;

    @Column(name = "referrer_email")
    @Email(message = "Invalid referrer email format")
    @Size(max = 255, message = "Referrer email must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referrerEmail;

    @Column(name = "reference_number", length = 100)
    @Size(max = 100, message = "Reference number must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String referenceNumber;

    @Column(name = "client_source", columnDefinition = "TEXT")
    @Size(max = 500, message = "Client source must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String clientSource; // How they heard about us

    @Column(name = "is_court_ordered")
    @Builder.Default
    private Boolean isCourtOrdered = false;

    @Column(name = "court_order_number")
    @Size(max = 255, message = "Court order number must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String courtOrderNumber;

    @Column(name = "court_jurisdiction")
    @Size(max = 255, message = "Court jurisdiction must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String courtJurisdiction;

    @Column(name = "requires_reporting")
    @Builder.Default
    private Boolean requiresReporting = false;

    @Column(name = "reporting_frequency", columnDefinition = "TEXT")
    @Size(max = 100, message = "Reporting frequency must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String reportingFrequency; // e.g., "Weekly", "Monthly", "Upon Completion"

    @Column(name = "reporting_recipient")
    @Size(max = 500, message = "Reporting recipient must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String reportingRecipient;

    @Column(name = "consent_to_contact_referrer")
    @Builder.Default
    private Boolean consentToContactReferrer = false;

    @Column(name = "marketing_campaign", columnDefinition = "TEXT")
    @Size(max = 255, message = "Marketing campaign must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String marketingCampaign; // Track marketing effectiveness

    @Column(name = "promo_code", columnDefinition = "TEXT")
    @Size(max = 100, message = "Promo code must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String promoCode;

    @Column(name = "referral_notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String referralNotes;

    @Column(name = "intake_summary", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String intakeSummary; // Initial presenting problem/reason for referral

    // Helper methods
    public boolean isCourtOrdered() {
        return Boolean.TRUE.equals(this.isCourtOrdered);
    }

    public boolean requiresReporting() {
        return Boolean.TRUE.equals(this.requiresReporting);
    }

    public boolean hasReferrer() {
        return this.referrerName != null && !this.referrerName.isBlank();
    }

    public String getReferrerDisplayName() {
        if (this.referrerName == null || this.referrerName.isBlank()) {
            return "Unknown Referrer";
        }

        StringBuilder sb = new StringBuilder(this.referrerName);

        if (this.referrerTitle != null && !this.referrerTitle.isBlank()) {
            sb.append(", ").append(this.referrerTitle);
        }

        if (this.referrerOrganization != null && !this.referrerOrganization.isBlank()) {
            sb.append(" (").append(this.referrerOrganization).append(")");
        }

        return sb.toString();
    }

    public boolean canContactReferrer() {
        return Boolean.TRUE.equals(this.consentToContactReferrer) && this.hasReferrer();
    }

    public String getReferralTypeDisplay() {
        if (this.isCourtOrdered()) {
            return "Court-Ordered";
        }

        return this.referralType != null ? this.referralType.name() : "Unknown";
    }
}
