package com.smart.therapy.flow.task.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class ReorderChecklistItemsRequest {
    @NotEmpty(message = "Item IDs are required")
    private List<Long> itemIds; // Ordered list of item IDs
}
