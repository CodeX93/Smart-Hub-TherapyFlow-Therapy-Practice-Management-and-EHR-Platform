package com.smart.therapy.flow.task.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.smart.therapy.flow.common.dto.PatchAwareRequest;
import com.smart.therapy.flow.common.jackson.PatchAwareRequestDeserializer;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;

@Data
@EqualsAndHashCode(callSuper = false)
@JsonDeserialize(using = PatchAwareRequestDeserializer.class)
public class UpdateTaskRequest extends PatchAwareRequest {

    private String title;
    private String titleKey;
    private String taskType;
    private String description;
    private String status;
    private String priority;
    private Long clientId;
    private Long assignedToId;
    private Instant dueDate;
}
