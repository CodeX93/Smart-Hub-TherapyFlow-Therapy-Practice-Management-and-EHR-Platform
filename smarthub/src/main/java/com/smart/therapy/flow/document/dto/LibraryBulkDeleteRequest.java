package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LibraryBulkDeleteRequest {

    @NotEmpty(message = "Entry IDs list cannot be empty")
    private List<@NotNull(message = "Entry ID cannot be null") Long> entryIds;
}

