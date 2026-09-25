package com.smart.therapy.flow.task.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChecklistItemResponse {
    private Long id;
    private Long templateId;
    private String title;
    private String description;
    private String category;
    private Boolean isRequired;
    private Integer itemOrder;
    private Integer daysFromStart;
    private Integer sortOrder;
    private Instant createdAt;
    private Instant updatedAt;
}

