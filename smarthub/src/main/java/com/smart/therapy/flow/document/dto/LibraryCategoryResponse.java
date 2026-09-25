package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class LibraryCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private Long parentId;
    private String parentName;
    private Integer sortOrder;
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}

