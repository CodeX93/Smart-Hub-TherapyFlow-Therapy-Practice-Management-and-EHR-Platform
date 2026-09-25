package com.smart.therapy.flow.system.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOptionCategoryRequest {
    @NotBlank(message = "Category key is required")
    private String categoryKey;
    
    @NotBlank(message = "Category name is required")
    private String categoryName;
    
    private String description;
    
    private Boolean isSystem;
    
    private Boolean isActive;
}

