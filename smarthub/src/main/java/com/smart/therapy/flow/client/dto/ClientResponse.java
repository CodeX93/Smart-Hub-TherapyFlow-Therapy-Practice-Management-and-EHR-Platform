package com.smart.therapy.flow.client.dto;

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
public class ClientResponse {

    private Long id;
    private String clientId;
    private String fullName;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String gender;
    private String maritalStatus;
    private String preferredLanguage;
    private String pronouns;
    /**
     * The timezone this client's times should be read in: their own setting when they
     * have one, otherwise the clinic's. Never blank, so the profile dropdown always has
     * something to show.
     */
    private String timezone;

    private String status;
    private String stage;
    private String clientType;

    private Long assignedTherapistId;
    private String assignedTherapistName;

    // Address
    private String streetAddress1;
    private String streetAddress2;
    private String city;
    private String province;
    private String postalCode;
    private String country;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;

    // Insurance
    private String insuranceProvider;
    private String policyNumber;
    private String groupNumber;
    private String insuranceType;
    private String insurancePhone;
    private BigDecimal copayAmount;
    private BigDecimal deductible;

    // Referral
    private String referrerName;
    private LocalDate referralDate;
    private LocalDate startDate;
    private String referenceNumber;
    private String clientSource;
    /** Referral note, from {@code client_referrals.referral_notes}; distinct from {@link #notes}. */
    private String referralNotes;

    // Portal
    private Boolean hasPortalAccess;
    private String portalEmail;
    private Boolean emailNotifications;
    /** Portal account last successful login ({@code auth_identities.last_successful_login}); null if never logged in. */
    private Instant lastLogin;

    // Employment / socioeconomic (from client_employment)
    private String employmentStatus;
    private String educationLevel;
    private Integer numberOfDependents;

    // Additional
    private String notes;
    private Boolean needsFollowUp;
    private String priority;
    private LocalDate followUpDate;
    private String followUpNotes;
    private String serviceType;
    private String serviceFrequency;
    private String treatmentModality;
    private Long checklistCount;
    private Long documentCount;

    // Timestamps
    private Instant createdAt;
    private Instant updatedAt;
    private Instant lastSessionDate;
    private Instant nextAppointmentDate;
}

