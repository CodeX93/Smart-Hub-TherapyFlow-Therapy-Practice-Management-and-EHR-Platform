package com.smart.therapy.flow.client.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Normalized insurance information for clients
 * Separates insurance data from core client profile
 * Benefits:
 * - Insurance data changes independently from client profile
 * - Can track insurance history and changes
 * - Can have different access controls for sensitive insurance data
 * - Easier to manage insurance-related features
 */
@Entity
@Table(name = "client_insurance", indexes = {
    @Index(name = "idx_client_insurance_client", columnList = "client_id"),
    @Index(name = "idx_client_insurance_is_active", columnList = "is_active")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ClientInsurance extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false, unique = true)
    @JsonIgnore
    private Client client;

    @Column(name = "insurance_provider", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Insurance provider is required")
    @Size(max = 500, message = "Insurance provider must not exceed 500 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String insuranceProvider;

    @Column(name = "insurance_type", columnDefinition = "TEXT")
    @Size(max = 100, message = "Insurance type must not exceed 100 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String insuranceType;

    @Column(name = "policy_number", nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Policy number is required")
    @Size(max = 255, message = "Policy number must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String policyNumber;

    @Column(name = "group_number", columnDefinition = "TEXT")
    @Size(max = 255, message = "Group number must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String groupNumber;

    @Column(name = "subscriber_name")
    @Size(max = 255, message = "Subscriber name must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String subscriberName;

    @Column(name = "subscriber_relationship", columnDefinition = "TEXT")
    @Size(max = 50, message = "Subscriber relationship must not exceed 50 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String subscriberRelationship; // Self, Spouse, Parent, Child, Other

    @Column(name = "insurance_phone", length = 20)
    @Pattern(regexp = "^[+]?[\\d\\s\\-()]+$", message = "Invalid insurance phone format")
    @Size(max = 20, message = "Insurance phone must not exceed 20 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String insurancePhone;

    @Column(name = "insurance_email")
    @Email(message = "Invalid insurance email format")
    @Size(max = 255, message = "Insurance email must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String insuranceEmail;

    @Column(name = "copay_amount", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Copay amount must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid copay amount format")
    private BigDecimal copayAmount;

    @Column(name = "deductible", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Deductible must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid deductible format")
    private BigDecimal deductible;

    @Column(name = "deductible_met", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Deductible met must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid deductible met format")
    @Builder.Default
    private BigDecimal deductibleMet = BigDecimal.ZERO;

    @Column(name = "out_of_pocket_max", precision = 10, scale = 2)
    @DecimalMin(value = "0.0", message = "Out of pocket max must be non-negative")
    @Digits(integer = 8, fraction = 2, message = "Invalid out of pocket max format")
    private BigDecimal outOfPocketMax;

    @Column(name = "coverage_percentage", precision = 5, scale = 2)
    @DecimalMin(value = "0.0", message = "Coverage percentage must be non-negative")
    @DecimalMax(value = "100.0", message = "Coverage percentage cannot exceed 100")
    @Digits(integer = 3, fraction = 2, message = "Invalid coverage percentage format")
    private BigDecimal coveragePercentage; // e.g., 80.00 for 80% coverage

    @Column(name = "effective_date")
    private LocalDate effectiveDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Column(name = "verified_at")
    private java.time.Instant verifiedAt;

    @Column(name = "verified_by", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String verifiedBy;

    @Column(name = "authorization_required")
    @Builder.Default
    private Boolean authorizationRequired = false;

    @Column(name = "authorization_number")
    @Size(max = 255, message = "Authorization number must not exceed 255 characters")
    @Convert(converter = EncryptedStringConverter.class)
    private String authorizationNumber;

    @Column(name = "authorization_expires_at")
    private LocalDate authorizationExpiresAt;

    @Column(name = "mental_health_coverage")
    @Builder.Default
    private Boolean mentalHealthCoverage = true;

    @Column(name = "telehealth_coverage")
    @Builder.Default
    private Boolean telehealthCoverage = false;

    @Column(name = "sessions_per_year")
    @Min(value = 0, message = "Sessions per year must be non-negative")
    private Integer sessionsPerYear;

    @Column(name = "sessions_used")
    @Builder.Default
    private Integer sessionsUsed = 0;

    @Column(name = "notes", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    // Helper methods
    public boolean isActive() {
        if (!Boolean.TRUE.equals(this.isActive)) {
            return false;
        }

        LocalDate now = LocalDate.now();
        
        if (this.effectiveDate != null && now.isBefore(this.effectiveDate)) {
            return false;
        }
        
        if (this.expiryDate != null && now.isAfter(this.expiryDate)) {
            return false;
        }
        
        return true;
    }

    public boolean hasRemainingessions() {
        if (this.sessionsPerYear == null) {
            return true; // Unlimited
        }
        
        return this.sessionsUsed < this.sessionsPerYear;
    }

    public Integer getRemainingSessions() {
        if (this.sessionsPerYear == null) {
            return null; // Unlimited
        }
        
        return Math.max(0, this.sessionsPerYear - (this.sessionsUsed != null ? this.sessionsUsed : 0));
    }

    public boolean isAuthorizationValid() {
        if (!Boolean.TRUE.equals(this.authorizationRequired)) {
            return true; // No authorization needed
        }
        
        if (this.authorizationNumber == null || this.authorizationNumber.isBlank()) {
            return false;
        }
        
        if (this.authorizationExpiresAt != null && LocalDate.now().isAfter(this.authorizationExpiresAt)) {
            return false;
        }
        
        return true;
    }

    public BigDecimal getRemainingDeductible() {
        if (this.deductible == null) {
            return BigDecimal.ZERO;
        }
        
        BigDecimal met = this.deductibleMet != null ? this.deductibleMet : BigDecimal.ZERO;
        return this.deductible.subtract(met).max(BigDecimal.ZERO);
    }

    public boolean isDeductibleMet() {
        if (this.deductible == null) {
            return true;
        }
        
        BigDecimal met = this.deductibleMet != null ? this.deductibleMet : BigDecimal.ZERO;
        return met.compareTo(this.deductible) >= 0;
    }
}

