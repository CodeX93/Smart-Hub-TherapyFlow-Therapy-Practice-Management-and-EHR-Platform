package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Age groups the therapist works with and has experience treating.
 */
@Entity
@Table(name = "user_profile_age_groups", indexes = {
        @Index(name = "idx_age_group_profile", columnList = "user_profile_id"),
        @Index(name = "idx_age_group_name", columnList = "age_group")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileAgeGroup extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "age_group", nullable = false, length = 100)
    private String ageGroup; // "Children (5-12)", "Adolescents (13-17)", "Adults (18-64)", "Seniors (65+)"

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience; // Years working with this age group

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
