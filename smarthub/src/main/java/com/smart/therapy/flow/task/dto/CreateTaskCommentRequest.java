package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateTaskCommentRequest {

    @NotBlank(message = "Content is required")
    private String content;

    private Boolean isInternal = false;
}

