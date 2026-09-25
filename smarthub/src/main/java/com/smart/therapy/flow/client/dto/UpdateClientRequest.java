package com.smart.therapy.flow.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
@Schema(description = "Request to update an existing client record. All fields are optional - only include fields you want to update. Send null to clear nullable fields.")
public class UpdateClientRequest extends PatchAwareRequest {

    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    @Schema(description = "Client's full name (optional)", example = "John Doe", maxLength = 255)
    private String fullName;

    @Email(message = "Invalid email format")
    @Schema(description = "Client's primary email address (optional)", example = "john.doe@example.com")
    private String email;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Client's phone number (optional)", example = "+1-555-123-4567", maxLength = 20)
    private String phone;

    @Schema(description = "Client's date of birth (optional)", example = "1990-05-15", type = "string", format = "date")
    private LocalDate dateOfBirth;

    @Schema(description = "Client's gender (optional)", example = "Male", allowableValues = {"Male", "Female", "Other", "Prefer not to say"})
    private String gender;

    @Schema(description = "Client's marital status (optional)", example = "Single", allowableValues = {"Single", "Married", "Divorced", "Widowed", "Separated"})
    private String maritalStatus;

    @Schema(description = "Client's preferred language (optional)", example = "English")
    private String preferredLanguage;

    @Schema(description = "Client's pronouns (optional)", example = "he/him")
    private String pronouns;

    @Schema(description = "Client timezone (IANA timezone ID, optional)", example = "America/New_York")
    private String timezone;

    @Schema(description = "Client status (optional)", example = "active", allowableValues = {"active", "inactive", "pending", "discharged"})
    private String status;

    @Schema(description = "Client stage in therapy process (optional)", example = "intake", allowableValues = {"intake", "assessment", "active treatment", "maintenance", "closed", "discharge"})
    private String stage;

    @Schema(description = "Type of client (optional)", example = "individual", allowableValues = {"individual", "group", "family"})
    private String clientType;

    @Schema(description = "ID of assigned therapist (optional)", example = "1")
    private Long assignedTherapistId;

    // Address fields
    private String streetAddress1;
    private String streetAddress2;
    private String city;
    private String province;
    private String postalCode;
    private String country;
    @JsonAlias({"legacyAddress"})
    @Schema(description = "Legacy single-line address format (optional)", example = "123 Main Street, Toronto, ON")
    private String addressLegacy;
    @Schema(description = "Legacy state field (optional)", example = "Ontario")
    private String stateLegacy;
    @Schema(description = "Legacy ZIP code field (optional)", example = "M5H 2N2")
    private String zipCodeLegacy;

    // Emergency contact
    private String emergencyContactName;
    private String emergencyContactPhone;
    private String emergencyContactRelationship;
    @Schema(description = "Legacy emergency contact text (optional)", example = "Jane Doe (Spouse) +1-555-987-6543")
    private String emergencyContactLegacy;

    // Insurance
    @Size(max = 500, message = "Insurance provider must not exceed 500 characters")
    private String insuranceProvider;
    @Schema(description = "Insurance type option key from insurance_types catalog", example = "private_insurance")
    private String insuranceType;
    @Size(max = 255, message = "Policy number must not exceed 255 characters")
    private String policyNumber;
    @Size(max = 255, message = "Group number must not exceed 255 characters")
    private String groupNumber;
    @Pattern(
            regexp = "^[+]?[\\d\\s\\-()]+$",
            message = "Insurance phone must contain only digits, spaces, dashes, parentheses, or a leading + sign")
    @Size(max = 20, message = "Insurance phone must not exceed 20 characters")
    private String insurancePhone;
    @DecimalMin(value = "0.0", message = "Copay amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid copay amount format")
    private BigDecimal copayAmount;
    @DecimalMin(value = "0.0", message = "Deductible must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid deductible format")
    private BigDecimal deductible;

    // Referral
    private String referrerName;
    @PastOrPresent(message = "Referral date must be in the past or present")
    private LocalDate referralDate;
    private String referenceNumber;
    private String clientSource;
    @Schema(description = "Legacy referral source text (optional)", example = "Website Campaign")
    private String legacyReferral;
    @Schema(description = "Legacy alias for referrer name (optional)", example = "Dr. Smith")
    private String referringPersonName;
    @Schema(description = "Referral type (optional)", example = "External")
    private String referralType;
    @Schema(description = "Referral notes (optional)", example = "Referred for anxiety intake")
    private String referralNotes;

    // Portal access
    private Boolean hasPortalAccess;
    private String portalEmail;
    private Boolean emailNotifications;

    // Additional
    private String notes;
    @Schema(description = "Legacy alias for notes (optional)", example = "Prefers morning appointments")
    private String generalNotes;
    private String serviceType;
    private String serviceFrequency;
    @Schema(description = "Treatment modality option key from treatment_modalities catalog", example = "cbt")
    private String treatmentModality;
    @JsonAlias({"startdate", "start_date"})
    @Schema(description = "Client start date (optional override)", example = "2026-04-23")
    private LocalDate startDate;
    @Schema(description = "Whether this client needs follow-up (optional)", example = "true")
    private Boolean needsFollowUp;
    @Schema(description = "Follow-up priority (optional)", example = "High")
    private String priority;
    @JsonAlias({"dueDate"})
    @Schema(description = "Follow-up due date (optional)", example = "2026-05-01")
    private LocalDate followUpDate;
    @Schema(description = "Follow-up notes (optional)", example = "Call after first session")
    private String followUpNotes;

    // Employment/Socioeconomic
    @Schema(description = "Employment status option key (optional)", example = "employed_full_time")
    private String employmentStatus;
    @Schema(description = "Education level option key (optional)", example = "bachelor")
    private String educationLevel;
    @JsonAlias({"numberOfDependents"})
    @Schema(description = "Number of dependents (optional)", example = "2")
    private Integer dependents;
}
