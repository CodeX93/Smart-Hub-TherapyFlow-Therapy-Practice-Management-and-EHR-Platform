package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * AssessmentReportVersion entity for tracking report version history.
 * Implements versioning for audit trail and compliance.
 * 
 * Best Practice: Maintain complete history of report changes for HIPAA compliance.
 */
@Entity
@Table(name = "assessment_report_versions", indexes = {
    @Index(name = "idx_report_version_report", columnList = "report_id"),
    @Index(name = "idx_report_version_created_at", columnList = "created_at"),
    @Index(name = "idx_report_version_version_number", columnList = "report_id, version_number")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentReportVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private AssessmentReport report;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber; // 1, 2, 3, etc.

    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    private String content; // Snapshot of report content at this version

    @Column(name = "change_type", length = 50, nullable = false)
    private String changeType; // GENERATED, EDITED, FINALIZED, REGENERATED

    @Column(name = "change_description", columnDefinition = "TEXT")
    private String changeDescription; // Description of what changed

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdByUser;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Check if this is the initial generated version
     */
    public boolean isInitialVersion() {
        return versionNumber == 1 && "GENERATED".equals(changeType);
    }

    /**
     * Check if this is a finalized version
     */
    public boolean isFinalized() {
        return "FINALIZED".equals(changeType);
    }
}
