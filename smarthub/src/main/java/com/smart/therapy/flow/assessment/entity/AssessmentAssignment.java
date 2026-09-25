package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.client.entity.Client;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_assignments", indexes = {
    @Index(name = "idx_assessment_assignment_client", columnList = "client_id"),
    @Index(name = "idx_assessment_assignment_template", columnList = "template_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentAssignment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private AssessmentTemplate template;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_by_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private User assignedBy;

    @Column(name = "assigned_date", nullable = false)
    @Builder.Default
    private Instant assignedDate = Instant.now(); // Explicit assignment timestamp

    @Column(nullable = false, length = 30)
    @Builder.Default
    private String status = "pending";

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "client_submitted_at")
    private Instant clientSubmittedAt;

    @Column(name = "therapist_completed_at")
    private Instant therapistCompletedAt;

    @Column(name = "total_score", precision = 10, scale = 2)
    private BigDecimal totalScore;

    @Column(columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String notes; // Therapist notes about the assessment

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AssessmentResponse> responses = new ArrayList<>();

    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AssessmentReport> reports = new ArrayList<>();
}
