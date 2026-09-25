package com.smart.therapy.flow.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ClientChecklistItemResponse {
    private Long id;
    private Long clientChecklistId;
    private Long checklistItemId;
    private String checklistItemTitle;
    private String checklistItemDescription;
    private String checklistItemCategory;
    private Boolean isCompleted;
    private Instant completedAt;
    private Long completedById;
    private String completedByName;
    private String notes;
    private Instant createdAt;
    private Instant updatedAt;
}

