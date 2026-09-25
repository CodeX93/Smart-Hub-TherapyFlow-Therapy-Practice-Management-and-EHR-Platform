package com.smart.therapy.flow.task.dto;

import lombok.Data;

@Data
public class CopyChecklistTemplateRequest {
    private String name; // Optional - if not provided, will use original name + " (Copy)"
    private String description; // Optional - if not provided, will use original description
    private Boolean isActive; // Optional - defaults to false
}
