package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.user.dto.BlockType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "therapist_blocked_times", indexes = {
    @Index(name = "idx_therapist_blocked_time", columnList = "therapist_id, start_time, end_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class TherapistBlockedTime extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;

    // Time Block Details
    @Column(name = "start_time", nullable = false)
    private Instant startTime;

    @Column(name = "end_time", nullable = false)
    private Instant endTime;

    @Column(name = "all_day", nullable = false)
    @Builder.Default
    private Boolean allDay = false;

    // Block Type & Reason
    @Enumerated(EnumType.STRING)
    @Column(name = "block_type", nullable = false, length = 50)
    private BlockType blockType; // vacation, meeting, sick_leave, personal, training, etc.

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "is_recurring", nullable = false)
    @Builder.Default
    private Boolean isRecurring = false;

    @Column(name = "recurrence_pattern", columnDefinition = "jsonb")
    private String recurrencePattern; // JSON for recurrence rules

    // Status
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}
