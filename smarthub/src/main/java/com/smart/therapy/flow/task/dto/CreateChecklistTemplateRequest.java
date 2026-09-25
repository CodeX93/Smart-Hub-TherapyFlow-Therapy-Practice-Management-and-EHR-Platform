package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.List;

@Data
public class CreateChecklistTemplateRequest {
    @NotBlank(message = "Name is required")
    private String name;
    
    private String description;
    
    private String clientType;
    
    private Boolean isActive;
    
    private Integer sortOrder;
    
    private List<CreateChecklistItemRequest> items;
}

