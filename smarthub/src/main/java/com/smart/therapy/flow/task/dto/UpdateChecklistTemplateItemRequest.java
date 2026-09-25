package com.smart.therapy.flow.task.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateChecklistTemplateItemRequest extends PatchAwareRequest {

    private String title;
    private String description;
    private String category;
    private Boolean isRequired;
    private Integer itemOrder;
    private Integer daysFromStart;
    private Integer sortOrder;
}
