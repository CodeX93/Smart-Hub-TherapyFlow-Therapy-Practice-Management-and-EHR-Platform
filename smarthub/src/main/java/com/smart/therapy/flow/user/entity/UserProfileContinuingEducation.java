package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Continuing education courses, workshops, and professional development.
 */
@Entity
@Table(name = "user_profile_continuing_education", indexes = {
        @Index(name = "idx_ce_profile", columnList = "user_profile_id"),
        @Index(name = "idx_ce_completion_date", columnList = "completion_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfileContinuingEducation extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "course_name", nullable = false, length = 255)
    private String courseName;

    @Column(length = 255)
    private String provider; // Institution or organization providing the training

    @Column(name = "completion_date")
    private LocalDate completionDate;

    @Column(name = "ce_credits")
    private Double ceCredits; // Continuing education credits earned

    @Column(name = "certificate_url", columnDefinition = "TEXT")
    private String certificateUrl; // Link to certificate

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
