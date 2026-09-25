package com.smart.therapy.flow.task.dto;

import lombok.Data;

@Data
public class UpdateTaskCommentRequest {
    private String content;
    private Boolean isInternal;
}

