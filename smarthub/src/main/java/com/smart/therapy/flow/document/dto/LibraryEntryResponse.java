package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class LibraryEntryResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String content;
    private List<String> tags;
    private Long createdById;
    private String createdByName;
    private Boolean isActive;
    private Integer sortOrder;
    private Integer usageCount;
    private Instant createdAt;
    private Instant updatedAt;
}

