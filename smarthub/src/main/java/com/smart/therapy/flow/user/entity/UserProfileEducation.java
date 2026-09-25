package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Educational background including degrees and institutions.
 */
@Entity
@Table(name = "user_profile_education", indexes = {
        @Index(name = "idx_education_profile", columnList = "user_profile_id"),
        @Index(name = "idx_education_degree", columnList = "degree_type")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileEducation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "degree_type", length = 100)
    private String degreeType; // "PhD", "PsyD", "MSW", "MA", "EdD"

    @Column(name = "field_of_study", length = 255)
    private String fieldOfStudy; // "Clinical Psychology", "Social Work", "Counseling"

    @Column(length = 255)
    private String institution;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    @Column(name = "graduation_date")
    private LocalDate graduationDate;

    @Column(name = "is_accredited")
    @Builder.Default
    private Boolean isAccredited = true; // Accredited institution

    @Column(name = "accreditation_body", length = 255)
    private String accreditationBody; // "APA", "CSWE", "CACREP"

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
