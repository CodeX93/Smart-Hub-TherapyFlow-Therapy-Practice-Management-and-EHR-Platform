package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Professional references for credentialing and verification.
 */
@Entity
@Table(name = "user_profile_references", indexes = {
        @Index(name = "idx_reference_profile", columnList = "user_profile_id"),
        @Index(name = "idx_reference_type", columnList = "reference_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileReference extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "reference_name", nullable = false, length = 255)
    private String referenceName;

    @Column(name = "reference_title", length = 255)
    private String referenceTitle; // Professional title

    @Column(name = "reference_organization", length = 255)
    private String referenceOrganization;

    @Column(name = "reference_type", length = 50)
    private String referenceType; // "Professional", "Academic", "Clinical Supervisor"

    @Column(length = 100)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "relationship", length = 255)
    private String relationship; // "Former Supervisor", "Colleague", "Professor"

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
