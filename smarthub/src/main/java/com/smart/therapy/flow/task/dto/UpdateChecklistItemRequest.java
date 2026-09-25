package com.smart.therapy.flow.task.dto;

import lombok.Data;

@Data
public class UpdateChecklistItemRequest {
    private Boolean isCompleted;
    
    private String notes;
}

