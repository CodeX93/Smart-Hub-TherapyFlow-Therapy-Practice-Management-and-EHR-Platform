package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Professional awards and recognitions.
 */
@Entity
@Table(name = "user_profile_awards", indexes = {
        @Index(name = "idx_award_profile", columnList = "user_profile_id"),
        @Index(name = "idx_award_year", columnList = "year_received")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileAward extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "award_name", nullable = false, length = 255)
    private String awardName;

    @Column(name = "awarding_organization", length = 255)
    private String awardingOrganization;

    @Column(name = "year_received")
    private Integer yearReceived;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
