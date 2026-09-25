package com.smart.therapy.flow.document.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a section within a form template version.
 * Sections organize fields into logical groups (e.g., "Personal Information", "Medical History").
 */
@Entity
@Table(name = "form_sections")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class FormSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_version_id", nullable = false)
    private FormTemplateVersion templateVersion;

    @Column(length = 200)
    private String name; // Section name (e.g., "Personal Information")

    @Column(columnDefinition = "TEXT")
    private String description; // Optional section description

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private List<FormField> fields = new ArrayList<>();
}
