package com.smart.therapy.flow.document.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateFormSectionRequest extends PatchAwareRequest {

    private String name;
    private String description;
    private Integer sortOrder;
}
