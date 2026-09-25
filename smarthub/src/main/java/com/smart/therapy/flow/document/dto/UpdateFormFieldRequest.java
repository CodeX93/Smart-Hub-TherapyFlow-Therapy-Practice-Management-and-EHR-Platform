package com.smart.therapy.flow.document.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateFormFieldRequest extends PatchAwareRequest {

    private String fieldType;
    private String label;
    private String placeholder;
    private String helpText;
    private Boolean isRequired;
    private String options;
    private String validation;
    private String defaultValue;
    private String autoPopulate;
    private String conditionalDisplay;
    private Integer sortOrder;
}
