package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Languages spoken by therapist with proficiency levels.
 * Enables client-therapist matching based on language needs.
 */
@Entity
@Table(name = "user_profile_languages", indexes = {
        @Index(name = "idx_language_profile", columnList = "user_profile_id"),
        @Index(name = "idx_language_name", columnList = "language"),
        @Index(name = "idx_language_proficiency", columnList = "proficiency_level")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileLanguage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(nullable = false, length = 100)
    private String language; // "English", "Spanish", "Mandarin", etc.

    @Enumerated(EnumType.STRING)
    @Column(name = "proficiency_level", nullable = false, length = 20)
    @Builder.Default
    private ProficiencyLevel proficiencyLevel = ProficiencyLevel.CONVERSATIONAL;

    @Column(name = "is_primary")
    @Builder.Default
    private Boolean isPrimary = false; // Primary language for therapy sessions

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false; // Verified by administrator

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by_id")
    private User verifiedBy;

    @Column(name = "verified_date")
    private LocalDate verifiedDate;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public enum ProficiencyLevel {
        BASIC, // Basic understanding
        CONVERSATIONAL, // Can hold basic conversations
        FLUENT, // Fluent, comfortable with complex topics
        NATIVE, // Native speaker
        MEDICAL_FLUENT // Fluent with medical/clinical terminology
    }

    public void verify(User admin) {
        this.isVerified = true;
        this.verifiedBy = admin;
        this.verifiedDate = LocalDate.now();
    }

    public boolean canProvideClinicalServices() {
        return proficiencyLevel == ProficiencyLevel.FLUENT
                || proficiencyLevel == ProficiencyLevel.NATIVE
                || proficiencyLevel == ProficiencyLevel.MEDICAL_FLUENT;
    }
}
