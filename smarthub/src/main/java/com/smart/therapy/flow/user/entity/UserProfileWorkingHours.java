package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;

/**
 * UserProfileWorkingHours - stores working hours for each day
 * Allows for multiple shifts per day
 */
@Entity
@Table(name = "user_profile_working_hours", indexes = {
        @Index(name = "idx_working_hours_profile_day", columnList = "user_profile_id, day"),
        @Index(name = "idx_working_hours_profile_day_service", columnList = "user_profile_id, day, service_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true, exclude = { "userProfile", "service" })
public class UserProfileWorkingHours extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_profile_id", nullable = false)
    private UserProfile userProfile;

    /**
     * When null, hours apply to the All-Services schedule.
     * When set (e.g. Consultation), hours apply only to that service's public/consultation schedule.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private com.smart.therapy.flow.billing.entity.Service service;

    @Column(nullable = false, length = 20)
    private String day; // 'MONDAY', 'TUESDAY', etc.

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "session_mode", nullable = false, length = 20)
    @Builder.Default
    private ShiftMode sessionMode = ShiftMode.BOTH;

    /**
     * Validate that end time is after start time
     */
    @PrePersist
    @PreUpdate
    private void validateWorkingHours() {
        if (startTime != null && endTime != null && !endTime.isAfter(startTime)) {
            throw new IllegalStateException("End time must be after start time for working hours");
        }
        if (sessionMode == null) {
            sessionMode = ShiftMode.BOTH;
        }

        if (day != null) {
            day = day.toUpperCase(); // Normalize to uppercase
            // Validate day name
            try {
                java.time.DayOfWeek.valueOf(day);
            } catch (IllegalArgumentException e) {
                throw new IllegalStateException("Invalid day name: " + day + ". Must be MONDAY, TUESDAY, etc.");
            }
        }
    }

    /**
     * Check if this working hours entry is valid
     */
    public boolean isValid() {
        return day != null && startTime != null && endTime != null && endTime.isAfter(startTime);
    }
}
