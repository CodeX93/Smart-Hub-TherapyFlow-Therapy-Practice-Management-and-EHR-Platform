package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Treatment approaches and therapeutic modalities.
 * Tracks which evidence-based practices the therapist uses.
 */
@Entity
@Table(name = "user_profile_treatment_approaches", indexes = {
        @Index(name = "idx_approach_profile", columnList = "user_profile_id"),
        @Index(name = "idx_approach_name", columnList = "approach"),
        @Index(name = "idx_approach_primary", columnList = "is_primary")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileTreatmentApproach extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, length = 255)
    private String approach; // "CBT", "DBT", "EMDR", "Psychodynamic", "Solution-Focused"

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", length = 20)
    @Builder.Default
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.COMPETENT;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // Primary therapeutic approach

    @Column(name = "years_practicing")
    private Integer yearsPracticing; // Years using this approach

    @Column(name = "formal_training")
    @Builder.Default
    private Boolean formalTraining = false; // Formally trained/certified

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ProficiencyLevel {
        FAMILIAR, // Basic knowledge
        COMPETENT, // Regularly uses
        PROFICIENT, // Highly experienced
        EXPERT // Certified/specialized
    }
}
