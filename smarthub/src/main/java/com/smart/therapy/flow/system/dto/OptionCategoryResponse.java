package com.smart.therapy.flow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

@Data
@Builder
public class OptionCategoryResponse {
    private Long id;
    private String categoryKey;
    private String categoryName;
    private String description;
    private Boolean isSystem;
    private Boolean isActive;
    private String ownershipType;
    private String optionSource;
    private Instant createdAt;
    private Instant updatedAt;
    private List<SystemOptionResponse> options;
}

