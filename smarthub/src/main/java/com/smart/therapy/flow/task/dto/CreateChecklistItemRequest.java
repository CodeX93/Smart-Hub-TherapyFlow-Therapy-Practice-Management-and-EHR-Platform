package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateChecklistItemRequest {
    @NotBlank(message = "Title is required")
    private String title;
    
    private String description;
    
    @NotBlank(message = "Category is required")
    private String category; // intake, assessment, ongoing, discharge
    
    private Boolean isRequired;
    
    private Integer itemOrder;
    
    private Integer daysFromStart;
    
    private Integer sortOrder;
}

