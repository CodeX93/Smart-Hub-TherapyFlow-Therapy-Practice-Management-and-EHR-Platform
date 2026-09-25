package com.smart.therapy.flow.task.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TaskCommentResponse {

    private Long id;
    private String content;
    private Boolean isInternal;
    private Long authorId;
    private String authorName;
    private Instant createdAt;
    @JsonProperty("totalCommentsCount")
    private long totalCommentsCount;
}

