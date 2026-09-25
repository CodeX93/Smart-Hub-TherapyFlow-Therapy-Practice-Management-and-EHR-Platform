package com.smart.therapy.flow.task.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class ChecklistTemplateResponse {
    private Long id;
    private String name;
    private String description;
    private String clientType;
    private Boolean isActive;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
    private Integer itemCount;
    private List<ChecklistItemResponse> items;
}

