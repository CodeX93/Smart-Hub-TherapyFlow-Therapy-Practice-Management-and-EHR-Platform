package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.auth.entity.User;
import com.smart.therapy.flow.common.entity.BaseEntity;
import com.smart.therapy.flow.document.enums.FormTemplateVersionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a version of a form template.
 * Allows templates to evolve over time while preserving historical versions.
 */
@Entity
@Table(name = "form_template_versions", indexes = {
    @Index(name = "idx_form_template_version_template", columnList = "template_id"),
    @Index(name = "idx_form_template_version_version", columnList = "version_number")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormTemplateVersion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private FormTemplate template;

    @Column(name = "version_number", nullable = false)
    private Long versionNumber; // Version number (1, 2, 3, ...)

    @Column(length = 200)
    private String name; // Optional override of template name

    @Column(columnDefinition = "TEXT")
    private String description; // Optional override of template description

    @Column(columnDefinition = "TEXT")
    private String instructions; // Optional override of template instructions

    @Column(name = "requires_signature")
    private Boolean requiresSignature; // Nullable for legacy rows; populated for new versions

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    @Builder.Default
    private FormTemplateVersionStatus status = FormTemplateVersionStatus.DRAFT;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdByUser;

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FormSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FormField> fields = new ArrayList<>();

    @OneToMany(mappedBy = "templateVersion", cascade = CascadeType.ALL)
    @Builder.Default
    private List<FormAssignment> assignments = new ArrayList<>();
}
