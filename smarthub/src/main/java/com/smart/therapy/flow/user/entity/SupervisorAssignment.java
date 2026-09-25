package com.smart.therapy.flow.user.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.user.dto.AssignmentType;
import com.smart.therapy.flow.user.dto.RequiredMeetingFrequency;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "supervisor_assignments", indexes = {
    @Index(name = "idx_supervisor_assignment_supervisor_active", columnList = "supervisor_id, is_active"),
    @Index(name = "idx_supervisor_assignment_therapist_active", columnList = "therapist_id, is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class SupervisorAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supervisor_id", nullable = false)
    private User supervisor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "therapist_id", nullable = false)
    private User therapist;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_type", nullable = false, length = 50)
    private AssignmentType assignmentType;
    
    @Column(name = "start_date", nullable = false)
    @Builder.Default
    private java.time.LocalDate startDate = java.time.LocalDate.now();
    
    @Column(name = "end_date")
    private java.time.LocalDate endDate;

    @Column(name = "assigned_date", nullable = false)
    @Builder.Default
    private Instant assignedDate = Instant.now();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Supervision Requirements
    @Enumerated(EnumType.STRING)
    @Column(name = "required_meeting_frequency", length = 50)
    private RequiredMeetingFrequency requiredMeetingFrequency; // Weekly, Bi-weekly

    @Column(name = "next_meeting_date")
    private Instant nextMeetingDate;

    @Column(name = "last_meeting_date")
    private Instant lastMeetingDate;

    @PrePersist
    @PreUpdate
    private void syncActiveStatus() {
        LocalDate today = LocalDate.now();
        boolean started = (this.startDate == null) || !this.startDate.isAfter(today);
        boolean notEnded = (this.endDate == null) || !this.endDate.isBefore(today);
        this.isActive = started && notEnded;
    }
}
