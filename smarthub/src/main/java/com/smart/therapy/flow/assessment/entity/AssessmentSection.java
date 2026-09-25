package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_sections")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentSection extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private AssessmentTemplate template;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "access_level", nullable = false, length = 20)
    @Builder.Default
    private String accessLevel = "therapist_only";

    @Column(name = "is_scoring")
    @Builder.Default
    private Boolean isScoring = false;

    @Column(name = "report_mapping", length = 50)
    private String reportMapping; // Maps to AI report sections

    @Column(name = "ai_report_prompt", columnDefinition = "TEXT")
    private String aiReportPrompt; // Optional AI prompt for report generation

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    @OneToMany(mappedBy = "section", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentQuestion> questions = new ArrayList<>();
}
