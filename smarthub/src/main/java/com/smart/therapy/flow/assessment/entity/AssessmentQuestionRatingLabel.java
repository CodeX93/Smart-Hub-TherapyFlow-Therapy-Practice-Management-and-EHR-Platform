package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "assessment_question_rating_labels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentQuestionRatingLabel extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String label;

    @Column
    private Integer score; // optional score for this label

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true; // default active
}

