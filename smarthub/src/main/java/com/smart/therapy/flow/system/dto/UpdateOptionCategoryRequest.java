package com.smart.therapy.flow.system.dto;

import lombok.Data;

@Data
public class UpdateOptionCategoryRequest {
    private String categoryKey;
    private String categoryName;
    private String description;
    private Boolean isActive;
}
