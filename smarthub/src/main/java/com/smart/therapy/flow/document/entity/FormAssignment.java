package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.Status;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "form_assignments", indexes = {
        @Index(name = "idx_form_assignment_client_status", columnList = "client_id, status"),
        @Index(name = "idx_form_assignment_template_version", columnList = "template_version_id"),
        @Index(name = "idx_form_assignment_assigned_by", columnList = "assigned_by_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private FormTemplateVersion templateVersion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    private User assignedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private Status status = Status.ASSIGNED; // 'ASSIGNED', 'IN_PROGRESS', 'SUBMITTED', 'COMPLETED', 'REVIEWED',
                                             // 'CANCELLED'

    @Column(name = "due_date")
    private Instant dueDate;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String instructions; // Custom instructions from therapist to client

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private User reviewedBy;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "reminders_sent", nullable = false)
    @Builder.Default
    private Integer remindersSent = 0;

    @Column(name = "last_reminder_at")
    private Instant lastReminderAt;

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FormAssignmentField> assignmentFields = new ArrayList<>(); // Field snapshots

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<FormResponse> responses = new ArrayList<>();

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    @Builder.Default
    private List<FormSignature> signatures = new ArrayList<>();
}
