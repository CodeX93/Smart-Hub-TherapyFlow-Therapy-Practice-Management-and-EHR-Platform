package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Professional certifications held by therapist.
 * Tracks expiry, renewal, and verification status for compliance.
 */
@Entity
@Table(name = "user_profile_certifications", indexes = {
        @Index(name = "idx_cert_profile", columnList = "user_profile_id"),
        @Index(name = "idx_cert_name", columnList = "certification_name"),
        @Index(name = "idx_cert_status", columnList = "status"),
        @Index(name = "idx_cert_expiry", columnList = "expiry_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileCertification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "certification_name", nullable = false, length = 255)
    private String certificationName; // "CBT Certified", "EMDR Trained", "Gottman Method", etc.

    @Column(name = "certifying_body", length = 255)
    private String certifyingBody; // "American Board of CBT", "EMDR International Association"

    @Column(name = "certification_number", length = 100)
    private String certificationNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private CertificationStatus status = CertificationStatus.PENDING;

    @Column(name = "certificate_document_url", columnDefinition = "TEXT")
    private String certificateDocumentUrl; // S3/cloud storage URL

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Column(name = "renewal_reminder_sent")
    @Builder.Default
    private Boolean renewalReminderSent = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum CertificationStatus {
        PENDING, // Submitted, awaiting verification
        VERIFIED, // Verified by admin
        EXPIRED, // Past expiry date
        REVOKED, // Certification revoked
        SUSPENDED // Temporarily suspended
    }

    public void verify(User admin) {
        this.status = CertificationStatus.VERIFIED;
        this.verifiedBy = admin;
        this.verifiedDate = LocalDate.now();
    }

    public boolean isExpiringSoon() {
        if (expiryDate == null)
            return false;
        return expiryDate.isBefore(LocalDate.now().plusMonths(3));
    }

    public boolean isExpired() {
        if (expiryDate == null)
            return false;
        return expiryDate.isBefore(LocalDate.now());
    }

    public boolean isActive() {
        return status == CertificationStatus.VERIFIED && !isExpired();
    }
}
