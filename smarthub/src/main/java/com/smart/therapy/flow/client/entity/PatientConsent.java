package com.smart.therapy.flow.client.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.enums.ConsentStatus;
import com.smart.therapy.flow.client.enums.ConsentType;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "patient_consents", indexes = {
        @Index(name = "idx_patient_consent_client", columnList = "client_id"),
        @Index(name = "idx_patient_consent_type", columnList = "consent_type"),
        @Index(name = "idx_patient_consent_status", columnList = "consent_status"),
        @Index(name = "idx_patient_consent_expiry", columnList = "expiry_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class PatientConsent extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @NotNull
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user", nullable = false)
    @NotNull
    private User createdByUser;

    // Consent Purpose/Type
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    private ConsentType consentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "consent_status", nullable = false, length = 50)
    private ConsentStatus consentStatus;

    @Column(name = "consent_date", nullable = false)
    private java.time.LocalDate consentDate;

    @Column(name = "expiry_date")
    private java.time.LocalDate expiryDate;

    @Column(name = "signed_by", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String signedBy;

    @Column(name = "signed_at")
    private Instant signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user")
    private User signedByUser;

    @Column(name = "witness_name", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String witnessName;

    @Column(name = "witness_signature", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String witnessSignature;

    @Column(name = "legal_guardian_name", length = 255)
    @Convert(converter = EncryptedStringConverter.class)
    private String legalGuardianName;

    @Column(name = "legal_guardian_relationship", length = 100)
    @Convert(converter = EncryptedStringConverter.class)
    private String legalGuardianRelationship;

    @Column(name = "legal_guardian_signature", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String legalGuardianSignature;

    // Unlike this entity's TEXT columns (and scheduled_notifications.entity_data),
    // signature_data is a genuine large-object column: V1__tenant_baseline.sql declares
    // it "oid" and live tenant schemas confirm it. @Lob is what routes Hibernate through
    // the large-object API here; without it the driver would return the oid pointer
    // number as the value. Keep @Lob unless a migration converts the column to text.
    @Lob
    @Column(name = "signature_data")
    @Convert(converter = EncryptedStringConverter.class)
    private String signatureData;

    @Column(name = "consent_document", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String consentDocument;

    @Column(name = "consent_form_url", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String consentFormUrl;

    @Column(name = "consent_form_version", length = 50)
    private String consentFormVersion;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes;

    // Consent Status
    @Column(nullable = false)
    @NotNull
    private Boolean granted; // true = consented, false = withdrawn

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private Instant grantedAt = Instant.now();

    @Column(name = "withdrawn_at")
    private Instant withdrawnAt;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    // Helper methods
    public boolean isHIPAAConsent() {
        return consentType != null && consentType.isHIPAARequired();
    }

    public boolean isRecordingConsent() {
        return consentType != null && consentType.isRecordingConsent();
    }

    public boolean requiresWitness() {
        return consentType == ConsentType.PARENTAL_CONSENT ||
                consentType == ConsentType.HIPAA_AUTHORIZATION;
    }

    public boolean isActive() {
        return Boolean.TRUE.equals(granted) && withdrawnAt == null;
    }

    public String getConsentTypeDisplay() {
        return consentType != null ? consentType.getDisplayName() : "Unknown";
    }
}
