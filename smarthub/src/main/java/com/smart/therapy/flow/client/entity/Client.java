package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.AuthIdentity;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.system.service.SystemOptionKeyMatcher;
import com.smart.therapy.flow.common.converter.EncryptedLocalDateConverter;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "clients", indexes = {
    @Index(name = "idx_client_id", columnList = "client_id"),
    @Index(name = "idx_clients_client_id_blind", columnList = "client_id_blind_idx"),
    @Index(name = "idx_clients_full_name_blind", columnList = "full_name_blind_idx"),
    @Index(name = "idx_client_auth_id", columnList = "auth_id"),
    @Index(name = "idx_client_status", columnList = "status"),
    @Index(name = "idx_client_stage", columnList = "stage"),
    @Index(name = "idx_client_therapist", columnList = "assigned_therapist_id"),
    @Index(name = "idx_clients_dob_blind", columnList = "date_of_birth_blind_idx"),
    @Index(name = "idx_client_last_session_date", columnList = "last_session_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Client extends BaseEntity {
    // NOTE: Optimistic locking @Version is defined once in BaseEntity.

    // After V57 + ENCRYPT_MRN backfill: TFENC:v2 ciphertext; uniqueness via client_id_blind_idx.
    @Column(name = "client_id", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Client ID is required")
    @Convert(converter = EncryptedStringConverter.class)
    private String clientId; // CL-YYYY-#### format (MRN) — encrypted at rest after cutover backfill

    @Column(name = "client_id_blind_idx", columnDefinition = "BYTEA")
    private byte[] clientIdBlindIdx;

    @EqualsAndHashCode.Include
    public Long getEntityIdForEquality() {
        return getId();
    }

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "auth_id") // nullable; not unique (indexed for lookups)
    @JsonIgnore
    private AuthIdentity authIdentity;

    // Personal Information (Tab 1)
    @Column(name = "full_name", nullable = false)
    @NotBlank(message = "Full name is required")
    @Size(max = 255, message = "Full name must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String fullName;

    @Column(name = "full_name_blind_idx", columnDefinition = "BYTEA")
    private byte[] fullNameBlindIdx;

    @Column(name = "date_of_birth", columnDefinition = "TEXT")
    @Past(message = "Date of birth must be in the past")
    @Convert(converter = EncryptedLocalDateConverter.class)
    private LocalDate dateOfBirth;

    @Column(name = "date_of_birth_blind_idx", columnDefinition = "BYTEA")
    private byte[] dateOfBirthBlindIdx;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String gender;

    @Column(name = "marital_status", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String maritalStatus;

    @Column(name = "preferred_language", columnDefinition = "TEXT")
    @Size(max = 50, message = "Preferred language must not exceed 50 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String preferredLanguage;

    @Column(columnDefinition = "TEXT")
    @Size(max = 20, message = "Pronouns must not exceed 20 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String pronouns;

    // Timezone (remains in Client for convenience)
    @Column(name = "timezone", length = 50)
    @Size(max = 50, message = "Timezone must not exceed 50 characters")
    private String timezone;

    // ============================================================================
    // NORMALIZED RELATIONSHIPS - Contact, Address, Insurance moved to separate tables
    // ============================================================================
    
    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ClientContact> contacts = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ClientNameBlindIndex> nameBlindIndexes = new ArrayList<>();

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ClientAddress> addresses = new ArrayList<>();

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private ClientInsurance insurance;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private ClientReferral referral;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private ClientEmployment employment;

    @OneToOne(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnore
    private ClientPortalSettings portalSettings;

    // Referral & Case Information
    @Column(name = "start_date")
    private LocalDate startDate;

    // Service Type & Frequency
    @Column(name = "service_type", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String serviceType;

    @Column(name = "service_frequency", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String serviceFrequency;

    @Column(name = "treatment_modality", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String treatmentModality;

    // Client Status & Progress (Tab 5)
    @Column(name = "client_type", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String clientType;

    @Column(length = 100)
    private String status;

    @Column(name = "stage", length = 100)
    private String stage;

    @Column(name = "last_update_date", nullable = false)
    @NotNull(message = "Last update date is required")
    @Builder.Default
    private Instant lastUpdateDate = Instant.now();

    // Follow-up Management
    @Column(name = "needs_follow_up")
    @Builder.Default
    private Boolean needsFollowUp = false;

    @Column(name = "follow_up_priority", length = 100)
    private String followUpPriority;

    @Column(name = "follow_up_date")
    private LocalDate followUpDate;

    @Column(name = "follow_up_notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String followUpNotes;

    // Duplicate Detection & Management
    @Column(name = "is_duplicate")
    @Builder.Default
    private Boolean isDuplicate = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_of_client_id")
    @JsonIgnore
    private Client duplicateOfClient;

    @Column(name = "duplicate_marked_at")
    private Instant duplicateMarkedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "duplicate_marked_by")
    @JsonIgnore
    private User duplicateMarkedBy;

    // Timestamps
    @Column(name = "last_session_date")
    private Instant lastSessionDate;

    @Column(name = "next_appointment_date")
    private Instant nextAppointmentDate;

    // Notes
    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_therapist_id")
    @JsonIgnore
    private User assignedTherapist;

    @OneToMany(mappedBy = "client", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @JsonIgnore
    private List<ClientHistory> history = new ArrayList<>();

    // Helper methods
    public void updateLastUpdateDate() {
        this.lastUpdateDate = Instant.now();
    }

    public boolean isActive() {
        return SystemOptionKeyMatcher.matchesAny(status, "active");
    }

    public boolean canReceiveServices() {
        return isActive() || SystemOptionKeyMatcher.matchesAny(status, "on_hold");
    }

    public boolean isFileClosed() {
        return SystemOptionKeyMatcher.matchesAny(status, "inactive");
    }

    public String getDisplayName() {
        return this.fullName;
    }


    /**
     * Get primary phone number from contacts
     */
    public String getPrimaryPhone() {
        if (this.contacts == null || this.contacts.isEmpty()) {
            return null;
        }
        return this.contacts.stream()
            .filter(c -> c.isPhone() && c.isPrimary())
            .findFirst()
            .map(ClientContact::getContactValue)
            .orElse(null);
    }

    /**
     * Get primary email from contacts
     */
    public String getPrimaryEmail() {
        if (this.contacts == null || this.contacts.isEmpty()) {
            return null;
        }
        return this.contacts.stream()
            .filter(c -> c.isEmail() && c.isPrimary())
            .findFirst()
            .map(ClientContact::getContactValue)
            .orElse(null);
    }

    /**
     * Get primary address
     */
    public ClientAddress getPrimaryAddress() {
        if (this.addresses == null || this.addresses.isEmpty()) {
            return null;
        }
        return this.addresses.stream()
            .filter(ClientAddress::isPrimary)
            .findFirst()
            .orElse(null);
    }

    /**
     * Get current age in years
     */
    public Integer getAge() {
        if (this.dateOfBirth == null) {
            return null;
        }
        return java.time.Period.between(this.dateOfBirth, LocalDate.now()).getYears();
    }

    /**
     * Check if client is a minor (under 18)
     */
    public boolean isMinor() {
        Integer age = getAge();
        return age != null && age < 18;
    }

    /**
     * Check if parental consent is required
     */
    public boolean requiresParentalConsent() {
        return isMinor();
    }
}
