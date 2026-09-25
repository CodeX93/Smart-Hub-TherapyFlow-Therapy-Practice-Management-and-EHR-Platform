package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateLibraryCategoryRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private Long parentId;
    
    private Integer sortOrder;
    
    private Boolean isActive;
}

