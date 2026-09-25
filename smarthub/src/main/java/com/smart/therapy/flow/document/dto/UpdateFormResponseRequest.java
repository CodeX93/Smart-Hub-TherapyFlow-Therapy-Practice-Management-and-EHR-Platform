package com.smart.therapy.flow.document.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateFormResponseRequest extends PatchAwareRequest {

    private String value;
}
