package com.smart.therapy.flow.client.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Schema(description = "Request to create a new client record. Note: id, clientId, createdAt, and updatedAt are system-generated and cannot be provided in this request.")
public class CreateClientRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name cannot exceed 255 characters")
    @Schema(description = "Client's full name", example = "John Doe", requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 255)
    private String fullName;

    @Email(message = "Invalid email format")
    @Schema(description = "Client's primary email address", example = "john.doe@example.com")
    private String email;

    @Size(max = 20, message = "Phone cannot exceed 20 characters")
    @Schema(description = "Client's phone number", example = "+1-555-123-4567", maxLength = 20)
    private String phone;

    @Schema(description = "Client's date of birth", example = "1990-05-15", type = "string", format = "date")
    private LocalDate dateOfBirth;

    @Schema(description = "Client's gender", example = "Male", allowableValues = {"Male", "Female", "Other", "Prefer not to say"})
    private String gender;

    @Schema(description = "Client's marital status", example = "Single", allowableValues = {"Single", "Married", "Divorced", "Widowed", "Separated"})
    private String maritalStatus;

    @Schema(description = "Client's preferred language", example = "English")
    private String preferredLanguage;

    @Schema(description = "Client's pronouns", example = "he/him")
    private String pronouns;

    @Schema(description = "Client timezone (IANA timezone ID)", example = "America/New_York")
    private String timezone;

    @NotNull(message = "Status is required")
    @Schema(description = "Client status", example = "active", requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"active", "inactive", "pending", "discharged"})
    private String status; // active, inactive, discharged

    @Schema(description = "Client stage in therapy process", example = "intake", allowableValues = {"intake", "active", "maintenance", "closed"})
    private String stage; // intake, active, maintenance, closed

    @Schema(description = "Type of client", example = "individual", allowableValues = {"individual", "group", "family"})
    private String clientType; // individual, group, family

    @Schema(description = "ID of assigned therapist", example = "1")
    private Long assignedTherapistId;

    // Address fields
    @Schema(description = "Street address line 1", example = "123 Main Street")
    private String streetAddress1;

    @Schema(description = "Street address line 2", example = "Apt 4B")
    private String streetAddress2;

    @Schema(description = "City", example = "Toronto")
    private String city;

    @Schema(description = "Province/State", example = "Ontario")
    private String province;

    @Schema(description = "Postal/ZIP code", example = "M5H 2N2")
    private String postalCode;

    @Schema(description = "Country", example = "Canada")
    private String country;

    @JsonAlias({"legacyAddress"})
    @Schema(description = "Legacy single-line address format", example = "123 Main Street, Toronto, ON")
    private String addressLegacy;

    @Schema(description = "Legacy state field", example = "Ontario")
    private String stateLegacy;

    @Schema(description = "Legacy ZIP code field", example = "M5H 2N2")
    private String zipCodeLegacy;

    // Emergency contact
    @Schema(description = "Emergency contact name", example = "Jane Doe")
    private String emergencyContactName;

    @Schema(description = "Emergency contact phone", example = "+1-555-987-6543")
    private String emergencyContactPhone;

    @Schema(description = "Relationship to client", example = "Spouse")
    private String emergencyContactRelationship;

    @Schema(description = "Legacy emergency contact text", example = "Jane Doe (Spouse) +1-555-987-6543")
    private String emergencyContactLegacy;

    // Insurance
    @Size(max = 500, message = "Insurance provider must not exceed 500 characters")
    @Schema(description = "Insurance provider option key from insurance_providers catalog", example = "blue_cross")
    private String insuranceProvider;

    @Schema(description = "Insurance type option key from insurance_types catalog", example = "private_insurance")
    private String insuranceType;

    @Size(max = 255, message = "Policy number must not exceed 255 characters")
    @Schema(description = "Insurance policy number", example = "POL123456789")
    private String policyNumber;

    @Size(max = 255, message = "Group number must not exceed 255 characters")
    @Schema(description = "Insurance group number", example = "GRP987654")
    private String groupNumber;

    @Pattern(
            regexp = "^[+]?[\\d\\s\\-()]+$",
            message = "Insurance phone must contain only digits, spaces, dashes, parentheses, or a leading + sign")
    @Size(max = 20, message = "Insurance phone must not exceed 20 characters")
    @Schema(description = "Insurance provider phone", example = "+1-800-555-1234")
    private String insurancePhone;

    @DecimalMin(value = "0.0", message = "Copay amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid copay amount format")
    @Schema(description = "Copay amount", example = "25.00", type = "number")
    private BigDecimal copayAmount;

    @DecimalMin(value = "0.0", message = "Deductible must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid deductible format")
    @Schema(description = "Deductible amount", example = "500.00", type = "number")
    private BigDecimal deductible;

    // Referral
    @Schema(description = "Name of person who referred the client", example = "Dr. Smith")
    private String referrerName;

    @Schema(description = "Date of referral", example = "2025-01-15", type = "string", format = "date")
    private LocalDate referralDate;

    @Schema(description = "Reference number", example = "REF-2025-001")
    private String referenceNumber;

    @Schema(description = "Source of client", example = "Website", allowableValues = {"Website", "Referral", "Walk-in", "Insurance", "Other"})
    private String clientSource;

    @Schema(description = "Legacy referral source text", example = "Website Campaign")
    private String legacyReferral;

    @Schema(description = "Legacy field alias for referrer name", example = "Dr. Smith")
    private String referringPersonName;

    @Schema(description = "Referral type", example = "External")
    private String referralType;

    @Schema(description = "Referral notes", example = "Referred for anxiety intake")
    private String referralNotes;

    // Portal access
    @Schema(description = "Whether client has portal access enabled", example = "false", defaultValue = "false")
    private Boolean hasPortalAccess = false;

    @Email(message = "Invalid portal email format")
    @Schema(description = "Portal email address (can be different from primary email). Required if hasPortalAccess is true.", example = "maria.garcia@example.com")
    private String portalEmail;

    @Schema(description = "Whether to send email notifications", example = "true", defaultValue = "true")
    private Boolean emailNotifications = true;

    // Additional
    @Schema(description = "Additional notes about the client", example = "Prefers morning appointments")
    private String notes;

    @Schema(description = "Legacy alias for notes", example = "Prefers morning appointments")
    private String generalNotes;

    @Schema(description = "Type of service", example = "Psychotherapy")
    private String serviceType;

    @Schema(description = "Service frequency", example = "Weekly", allowableValues = {"Weekly", "Bi-weekly", "Monthly", "As needed"})
    private String serviceFrequency;

    @Schema(description = "Treatment modality option key from treatment_modalities catalog", example = "cbt")
    private String treatmentModality;

    @JsonAlias({"startdate", "start_date"})
    @Schema(description = "Client start date (optional override; defaults to current date)", example = "2026-04-23", type = "string", format = "date")
    private LocalDate startDate;

    @Schema(description = "Whether this client needs follow-up", example = "true")
    private Boolean needsFollowUp;

    @Schema(description = "Follow-up priority", example = "High")
    private String priority;

    @JsonAlias({"dueDate"})
    @Schema(description = "Follow-up due date", example = "2026-05-01", type = "string", format = "date")
    private LocalDate followUpDate;

    @Schema(description = "Follow-up notes", example = "Call after first session")
    private String followUpNotes;

    // Employment/Socioeconomic (accepted directly during create for compatibility)
    @Schema(description = "Employment status option key", example = "employed_full_time")
    private String employmentStatus;

    @Schema(description = "Education level option key", example = "bachelor")
    private String educationLevel;

    @JsonAlias({"numberOfDependents"})
    @Schema(description = "Number of dependents", example = "2")
    private Integer dependents;
    
    // Golden Rule: Idempotency key to prevent duplicate client creation from double-clicks
    @Schema(description = "Idempotency key (optional). If provided and a client was already created with this key, the existing client will be returned instead of creating a new one.", example = "550e8400-e29b-41d4-a716-446655440000")
    private String idempotencyKey;
}

