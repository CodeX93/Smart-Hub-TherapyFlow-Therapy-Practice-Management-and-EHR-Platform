package com.smart.therapy.flow.assessment.entity;

import com.smart.therapy.flow.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "assessment_response_options", indexes = {
        @Index(name = "idx_response_option_response", columnList = "response_id"),
        @Index(name = "idx_response_option_option", columnList = "option_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = true)
public class AssessmentResponseOption extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "response_id", nullable = false)
    private AssessmentResponse response;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_id", nullable = false)
    private AssessmentQuestionOption option; // Reference to the question's option

    @Column(precision = 10, scale = 2)
    private BigDecimal score; // Optional score per selected option

    @Column(columnDefinition = "TEXT")
    private String notes; // Optional notes per option
}

