package com.smart.therapy.flow.assessment.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
public class AssessmentResponseDto {

    private Long id;
    private Long assignmentId;
    private Long questionId;
    private String questionText;

    private String responderType;
    private Long responderUserId;
    private Long responderClientId;

    private String responseText;
    private String responseValue;
    private BigDecimal score;
    private Instant answeredAt;

    private List<Long> selectedOptionIds;
    private Integer ratingValue;
}

