package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReorderFormFieldsRequest {

    /**
     * List of field IDs in the desired order.
     * The first ID will get sortOrder = 0, second = 1, etc.
     */
    @NotNull(message = "Field IDs are required")
    private List<Long> fieldIds;
}

