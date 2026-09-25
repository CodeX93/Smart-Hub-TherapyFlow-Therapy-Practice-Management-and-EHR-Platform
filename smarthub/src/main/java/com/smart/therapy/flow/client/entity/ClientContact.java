package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.client.enums.ContactType;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

/**
 * Normalized contact information for clients
 * Supports multiple contacts per client (phone, email, emergency contacts)
 * Benefits:
 * - Can have multiple phone numbers (mobile, home, work)
 * - Can have multiple email addresses
 * - Can have multiple emergency contacts with full details
 * - Each contact can be individually validated and managed
 */
@Entity
@Table(name = "client_contacts", indexes = {
    @Index(name = "idx_client_contact_client", columnList = "client_id"),
    @Index(name = "idx_client_contact_type", columnList = "contact_type"),
    @Index(name = "idx_client_contact_is_primary", columnList = "is_primary"),
    @Index(name = "idx_client_contacts_contact_blind", columnList = "contact_blind_idx")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientContact extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "contact_type", nullable = false, length = 30)
    @NotNull(message = "Contact type is required")
    private ContactType contactType;

    @Column(name = "contact_value", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Contact value is required")
    @Size(max = 500, message = "Contact value must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String contactValue; // Phone number or email address

    @Column(name = "contact_blind_idx", columnDefinition = "BYTEA")
    private byte[] contactBlindIdx;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "label", columnDefinition = "TEXT")
    @Size(max = 100, message = "Label must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String label; // e.g., "Mobile", "Home", "Work", "Personal"

    // Emergency contact specific fields
    @Column(name = "contact_person_name", columnDefinition = "TEXT")
    @Size(max = 255, message = "Contact person name must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String contactPersonName;

    @Column(name = "relationship", columnDefinition = "TEXT")
    @Size(max = 100, message = "Relationship must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String relationship; // e.g., "Spouse", "Parent", "Sibling"

    @Column(name = "can_make_medical_decisions")
    @Builder.Default
    private Boolean canMakeMedicalDecisions = false;

    @Column(name = "can_be_notified")
    @Builder.Default
    private Boolean canBeNotified = true;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // Helper methods
    public boolean isPrimary() {
        return Boolean.TRUE.equals(this.isPrimary);
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    public boolean isEmergencyContact() {
        return this.contactType == ContactType.EMERGENCY_CONTACT;
    }

    public boolean isPhone() {
        return this.contactType != null && this.contactType.isPhone();
    }

    public boolean isEmail() {
        return this.contactType != null && this.contactType.isEmail();
    }

    public String getDisplayValue() {
        if (this.isEmergencyContact() && this.contactPersonName != null) {
            return String.format("%s (%s) - %s", this.contactPersonName, this.relationship, this.contactValue);
        }
        
        if (this.label != null) {
            return String.format("%s: %s", this.label, this.contactValue);
        }
        
        return this.contactValue;
    }

    /**
     * Validate contact value based on type
     */
    public boolean isValidForType() {
        if (this.contactType == null || this.contactValue == null) {
            return false;
        }

        switch (this.contactType) {
            case EMAIL:
                return this.contactValue.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
            case PHONE:
            case WORK_PHONE:
                return this.contactValue.matches("^[+]?[\\d\\s\\-()]+$");
            case EMERGENCY_CONTACT:
                return this.contactPersonName != null && !this.contactPersonName.isBlank();
            default:
                return true;
        }
    }
}

