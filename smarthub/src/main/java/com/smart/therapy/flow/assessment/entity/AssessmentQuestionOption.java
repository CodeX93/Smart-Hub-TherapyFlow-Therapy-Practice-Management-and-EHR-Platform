package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "assessment_question_options")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentQuestionOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private AssessmentQuestion question;

    @Column(name = "option_key", length = 100)
    private String optionKey; // Unique identifier for the option

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "option_value", length = 100)
    private String optionValue; // Value stored when this option is selected

    @Column(name = "score_value", precision = 10, scale = 2)
    private BigDecimal scoreValue; // Numeric score for this option

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "is_default")
    private Boolean isDefault;
}
