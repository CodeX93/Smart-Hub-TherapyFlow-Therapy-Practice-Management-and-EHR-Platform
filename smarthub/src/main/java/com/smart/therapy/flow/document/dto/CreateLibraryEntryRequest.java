package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CreateLibraryEntryRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;
    
    @NotBlank(message = "Title is required")
    private String title;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    private List<String> tags;
    
    private Integer sortOrder;
    
    private Boolean isActive;
}

