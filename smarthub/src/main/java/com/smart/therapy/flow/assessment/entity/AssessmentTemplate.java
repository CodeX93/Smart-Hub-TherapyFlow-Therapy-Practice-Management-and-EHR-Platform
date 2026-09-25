package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentTemplate extends BaseEntity {

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String category;

    @Column(name = "is_standardized")
    @Builder.Default
    private Boolean isStandardized = false;

    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private com.smart.therapy.flow.auth.entity.User createdByUser;

    @Column(name = "version_number", nullable = false)
    @Builder.Default
    private Integer versionNumber = 1; // Numeric version: 1, 2, 3, etc.

    // Unique constraint on (name, version_number) - enforced at database level

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AssessmentSection> sections = new ArrayList<>();

    @OneToMany(mappedBy = "template", cascade = CascadeType.ALL)
    @Builder.Default
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<AssessmentAssignment> assignments = new ArrayList<>();
}


