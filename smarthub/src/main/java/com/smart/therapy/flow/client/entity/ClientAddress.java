package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.client.enums.AddressType;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Normalized address information for clients
 * Supports multiple addresses per client (home, work, billing, temporary)
 * Benefits:
 * - Can have separate home, work, and billing addresses
 * - Can track address history and changes
 * - Can have temporary or seasonal addresses
 * - Each address can be individually validated and managed
 */
@Entity
@Table(name = "client_addresses", indexes = {
    @Index(name = "idx_client_address_client", columnList = "client_id"),
    @Index(name = "idx_client_address_type", columnList = "address_type"),
    @Index(name = "idx_client_address_is_primary", columnList = "is_primary")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientAddress extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @JsonIgnore
    private Client client;

    @Enumerated(EnumType.STRING)
    @Column(name = "address_type", nullable = false, length = 20)
    @NotNull(message = "Address type is required")
    private AddressType addressType;

    @Column(name = "street_address_1", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Street address is required")
    @Size(max = 500, message = "Street address must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String streetAddress1;

    @Column(name = "street_address_2", columnDefinition = "TEXT")
    @Size(max = 500, message = "Street address 2 must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String streetAddress2;

    @Column(name = "city", nullable = false, length = 100)
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String city;

    @Column(name = "state_province", length = 100)
    @Size(max = 100, message = "State/Province must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String stateProvince;

    @Column(name = "postal_code", length = 20)
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String postalCode;

    @Column(name = "country", nullable = false, length = 100)
    @NotBlank(message = "Country is required")
    @Size(max = 100, message = "Country must not exceed 100 characters")
    @Builder.Default
    @Convert(converter = EncryptedStringConverter.class)
    private String country = "United States";

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = true;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    @Column(name = "display_order")
    @Builder.Default
    private Integer displayOrder = 0;

    // ============================================================================
    // LEGACY FIELDS - For backward compatibility during migration
    // These fields support legacy address formats from old system
    // ============================================================================

    @Column(name = "address_legacy", columnDefinition = "TEXT")
    @Size(max = 500, message = "Legacy address must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String addressLegacy; // Legacy single-line address format

    @Column(name = "state_legacy", length = 50)
    @Size(max = 50, message = "Legacy state must not exceed 50 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String stateLegacy; // Legacy state field (mapped to stateProvince)

    @Column(name = "zip_code_legacy", length = 10)
    @Size(max = 10, message = "Legacy zip code must not exceed 10 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String zipCodeLegacy; // Legacy zip code field (mapped to postalCode)

    // Helper methods
    public boolean isPrimary() {
        return Boolean.TRUE.equals(this.isPrimary);
    }

    public boolean isCurrent() {
        return Boolean.TRUE.equals(this.isCurrent);
    }

    public boolean isVerified() {
        return Boolean.TRUE.equals(this.isVerified);
    }

    public boolean isActive() {
        if (!this.isCurrent()) {
            return false;
        }

        LocalDate now = LocalDate.now();
        
        if (this.validFrom != null && now.isBefore(this.validFrom)) {
            return false;
        }
        
        if (this.validUntil != null && now.isAfter(this.validUntil)) {
            return false;
        }
        
        return true;
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(this.streetAddress1);
        
        if (this.streetAddress2 != null && !this.streetAddress2.isBlank()) {
            sb.append(", ").append(this.streetAddress2);
        }
        
        sb.append(", ").append(this.city);
        
        if (this.stateProvince != null && !this.stateProvince.isBlank()) {
            sb.append(", ").append(this.stateProvince);
        }
        
        if (this.postalCode != null && !this.postalCode.isBlank()) {
            sb.append(" ").append(this.postalCode);
        }
        
        sb.append(", ").append(this.country);
        
        return sb.toString();
    }

    public String getShortAddress() {
        return String.format("%s, %s, %s", this.streetAddress1, this.city, this.country);
    }

    public String getDisplayName() {
        return String.format("%s Address: %s", 
            this.addressType != null ? this.addressType.getDisplayName() : "Unknown",
            this.getShortAddress());
    }
}

