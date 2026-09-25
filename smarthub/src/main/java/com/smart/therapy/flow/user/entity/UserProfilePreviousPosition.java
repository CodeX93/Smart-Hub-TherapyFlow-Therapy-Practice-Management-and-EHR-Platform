package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

/**
 * Previous professional positions and work history.
 */
@Entity
@Table(name = "user_profile_previous_positions", indexes = {
        @Index(name = "idx_prev_position_profile", columnList = "user_profile_id"),
        @Index(name = "idx_prev_position_start", columnList = "start_date"),
        @Index(name = "idx_prev_position_end", columnList = "end_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class UserProfilePreviousPosition extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    @Column(name = "job_title", nullable = false, length = 255)
    private String jobTitle;

    @Column(length = 255)
    private String organization;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "is_current")
    @Builder.Default
    private Boolean isCurrent = false;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(columnDefinition = "TEXT")
    private String responsibilities;

    @Column(columnDefinition = "TEXT")
    private String notes;

    public Integer getYearsInRole() {
        if (startDate == null)
            return null;
        LocalDate end = endDate != null ? endDate : LocalDate.now();
        return end.getYear() - startDate.getYear();
    }
}
