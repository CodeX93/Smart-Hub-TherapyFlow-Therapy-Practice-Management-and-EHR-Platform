package com.smart.therapy.flow.system.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class SystemOptionResponse {
    private Long id;
    private Long categoryId;
    private String categoryKey;
    private String categoryName;
    private String optionKey;
    private String optionLabel;
    private Integer sortOrder;
    private Boolean isDefault;
    private Boolean isSystem;
    private Boolean isActive;
    private BigDecimal price;
    private Instant createdAt;
    private Instant updatedAt;
}

