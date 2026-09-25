package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Professional organization memberships.
 */
@Entity
@Table(name = "user_profile_memberships", indexes = {
        @Index(name = "idx_membership_profile", columnList = "user_profile_id"),
        @Index(name = "idx_membership_status", columnList = "status")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "organization_name", nullable = false, length = 255)
    private String organizationName; // "APA", "NASW", "ACA", "AAMFT"

    @Column(name = "membership_number", length = 100)
    private String membershipNumber;

    @Column(name = "membership_type", length = 100)
    private String membershipType; // "Full Member", "Student Member", "Fellow"

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private MembershipStatus status = MembershipStatus.ACTIVE;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum MembershipStatus {
        ACTIVE,
        INACTIVE,
        PENDING,
        EXPIRED
    }
}
