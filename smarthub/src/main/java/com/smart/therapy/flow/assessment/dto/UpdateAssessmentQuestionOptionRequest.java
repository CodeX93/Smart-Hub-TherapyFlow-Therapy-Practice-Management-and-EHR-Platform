package com.smart.therapy.flow.assessment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateAssessmentQuestionOptionRequest extends PatchAwareRequest {

    private String optionKey;
    private String optionText;
    private String optionValue;
    private BigDecimal scoreValue;
    private Integer sortOrder;
    private Boolean isDefault;
}
