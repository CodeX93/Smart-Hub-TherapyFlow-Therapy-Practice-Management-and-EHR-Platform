package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Clinical specializations with expertise level and experience tracking.
 * Used for therapist-client matching and expertise verification.
 */
@Entity
@Table(name = "user_profile_specializations", indexes = {
        @Index(name = "idx_spec_profile", columnList = "user_profile_id"),
        @Index(name = "idx_spec_name", columnList = "specialization"),
        @Index(name = "idx_spec_primary", columnList = "is_primary"),
        @Index(name = "idx_spec_years", columnList = "years_of_experience")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileSpecialization extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, length = 255)
    private String specialization; // "Trauma", "Anxiety", "Depression", "Addiction", "PTSD"

    @Enumerated(EnumType.STRING)
    @Column(name = "expertise_level", length = 20)
    @Builder.Default
    private ExpertiseLevel expertiseLevel = ExpertiseLevel.INTERMEDIATE;

    @Column(name = "years_of_experience")
    private Integer yearsOfExperience; // Years practicing this specialization

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // Primary specialization

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String description; // Details about expertise in this area

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ExpertiseLevel {
        BASIC, // < 2 years
        INTERMEDIATE, // 2-5 years
        ADVANCED, // 5-10 years
        EXPERT // 10+ years or specialized certification
    }

    public boolean isExpert() {
        return expertiseLevel == ExpertiseLevel.EXPERT
                || (yearsOfExperience != null && yearsOfExperience >= 10);
    }
}
