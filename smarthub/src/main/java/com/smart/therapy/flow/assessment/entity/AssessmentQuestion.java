package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "assessment_questions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentQuestion extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "section_id", nullable = false)
    private AssessmentSection section;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "question_type", nullable = false, length = 50)
    private String questionType;

    @Column(name = "is_required")
    @Builder.Default
    private Boolean isRequired = false;

    @Column(name = "sort_order")
    @Builder.Default
    private Integer sortOrder = 0;

    // For rating scales
    @Column(name = "rating_min")
    private Integer ratingMin;

    @Column(name = "rating_max")
    private Integer ratingMax;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentQuestionRatingLabel> ratingLabels = new ArrayList<>(); // ["Poor", "Good", "Excellent"]

    // Configuration for scoring
    @Column(name = "contributes_to_score")
    @Builder.Default
    private Boolean contributesToScore = false;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AssessmentQuestionOption> options = new ArrayList<>();

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL)
    @Builder.Default
    private List<AssessmentResponse> responses = new ArrayList<>();
}
