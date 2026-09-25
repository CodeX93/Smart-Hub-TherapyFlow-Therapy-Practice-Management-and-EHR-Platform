package com.smart.therapy.flow.assessment.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.converter.EncryptedStringConverter;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.common.exception.BusinessLogicException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Entity
@Table(name = "assessment_reports", indexes = {
    @Index(name = "idx_assessment_report_assignment", columnList = "assignment_id"),
    @Index(name = "idx_assessment_report_status", columnList = "is_draft,is_finalized")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentReport extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignment_id", nullable = false)
    @NotNull(message = "Assessment assignment is required")
    @JsonIgnore
    private AssessmentAssignment assignment;

    // Content fields (matching session notes pattern)
    @Column(name = "generated_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String generatedContent; // AI-generated report content (read-only)

    @Column(name = "draft_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String draftContent; // Editable draft version

    @Column(name = "final_content", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String finalContent; // Locked finalized version

    @Column(name = "report_data", columnDefinition = "TEXT")
    @Convert(converter = EncryptedStringConverter.class)
    private String reportData; // JSON structure of organized data

    @Column(name = "template_snapshot", columnDefinition = "JSONB")
    @JdbcTypeCode(SqlTypes.JSON)
    private String templateSnapshot; // JSON snapshot of template structure at report generation time

    // Status tracking (matching session notes pattern)
    @Column(name = "is_draft", nullable = false)
    @NotNull(message = "Draft status is required")
    @Builder.Default
    private Boolean isDraft = true;

    @Column(name = "is_finalized", nullable = false)
    @NotNull(message = "Finalized status is required")
    @Builder.Default
    private Boolean isFinalized = false;

    // Timestamps (matching session notes pattern)
    @Column(name = "generated_at")
    private Instant generatedAt;

    @Column(name = "edited_at")
    private Instant editedAt;

    @Column(name = "finalized_at")
    private Instant finalizedAt;

    @Column(name = "exported_at")
    private Instant exportedAt;

    // User tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @NotNull(message = "Created by user is required")
    @JsonIgnore
    private User createdByUser;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "finalized_by_id")
    @JsonIgnore
    private User finalizedByUser;

    // Helper methods
    public void finalizeReport(User finalizedBy) {
        if (Boolean.TRUE.equals(this.isFinalized)) {
            throw new BusinessLogicException("Report is already finalized");
        }
        this.isFinalized = true;
        this.isDraft = false;
        this.finalizedAt = Instant.now();
        this.finalizedByUser = finalizedBy;
        if (this.draftContent != null) {
            this.finalContent = this.draftContent;
        }
    }

    public boolean canEdit() {
        return !Boolean.TRUE.equals(this.isFinalized);
    }

    public void updateDraft(String content) {
        if (!canEdit()) {
            throw new BusinessLogicException("Cannot edit finalized report");
        }
        this.draftContent = content;
        this.editedAt = Instant.now();
    }

    public void markAsGenerated() {
        this.generatedAt = Instant.now();
    }

    public void markAsExported() {
        this.exportedAt = Instant.now();
    }

    public boolean isEdited() {
        return this.editedAt != null;
    }

    public boolean isExported() {
        return this.exportedAt != null;
    }
}
