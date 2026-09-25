package com.smart.therapy.flow.report.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "report_templates", indexes = {
        @Index(name = "idx_report_templates_active", columnList = "is_active")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class ReportTemplate extends BaseEntity {

    @Column(nullable = false, length = 255)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "ai_instructions", columnDefinition = "TEXT")
    private String aiInstructions;

    @Column(name = "original_name", length = 500)
    private String originalName;

    @Column(name = "mime_type", length = 150)
    private String mimeType;

    @Column(name = "file_size")
    private Integer fileSize;

    @Column(name = "file_blob_name", length = 1000)
    private String fileBlobName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "structure_text", columnDefinition = "TEXT")
    private String structureText;

    @Column(name = "default_include_profile", nullable = false)
    @Builder.Default
    private Boolean defaultIncludeProfile = true;

    @Column(name = "default_include_notes", nullable = false)
    @Builder.Default
    private Boolean defaultIncludeNotes = true;

    @Column(name = "default_include_assessments", nullable = false)
    @Builder.Default
    private Boolean defaultIncludeAssessments = true;

    @Column(name = "supporting_files_guidance", columnDefinition = "TEXT")
    private String supportingFilesGuidance;

    @Column(name = "supporting_files_expected", nullable = false)
    @Builder.Default
    private Boolean supportingFilesExpected = false;

    /** JSON array of allowed document type labels, e.g. ["GP letter","Intake form"] */
    @Column(name = "supporting_file_types", columnDefinition = "TEXT")
    private String supportingFileTypesJson;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @JsonIgnore
    private User createdByUser;
}
