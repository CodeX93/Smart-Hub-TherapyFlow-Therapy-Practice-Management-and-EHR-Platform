package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.SignatureType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "form_signatures", indexes = {
        @Index(name = "idx_form_signature_assignment", columnList = "assignment_id"),
        @Index(name = "idx_form_signature_signed_at", columnList = "signed_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormSignature extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    private FormAssignment assignment;

    /**
     * Base64 encoded signature image.
     * 
     * SECURITY NOTE: This field contains sensitive biometric data (signature
     * images).
     * For HIPAA compliance, this data should be encrypted at rest.
     * 
     * Recommended implementations:
     * 1. Use JPA field-level encryption (e.g., Jasypt, Vault)
     * 2. Move to encrypted blob storage (e.g., Azure Blob Storage with encryption,
     * AWS S3 with KMS)
     * 3. Use database-level encryption (e.g., PostgreSQL pgcrypto, Transparent Data
     * Encryption)
     * 
     * Current status: Stored as plain text - encryption required before production
     * deployment.
     */
    @Column(name = "signature_data", nullable = false, columnDefinition = "TEXT")
    @Convert(converter = com.smart.therapy.flow.common.converter.EncryptedStringConverter.class)
    private String signatureData; // Encrypted PHI (Base64 signature)

    @Column(name = "signer_name", nullable = false, length = 255)
    @Convert(converter = com.smart.therapy.flow.common.converter.EncryptedStringConverter.class)
    private String signerName;

    @Column(name = "signer_email", length = 255)
    @Convert(converter = com.smart.therapy.flow.common.converter.EncryptedStringConverter.class)
    private String signerEmail;

    @Column(name = "signer_role", length = 50)
    private String signerRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "signature_type", nullable = false, length = 50)
    private SignatureType signatureType; // 'drawn', 'typed', 'uploaded'

    @Column(name = "signed_at", nullable = false)
    @Builder.Default
    private Instant signedAt = Instant.now();

    @Column(name = "agreed_to_terms", nullable = false)
    @Builder.Default
    private Boolean agreedToTerms = false;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
}
