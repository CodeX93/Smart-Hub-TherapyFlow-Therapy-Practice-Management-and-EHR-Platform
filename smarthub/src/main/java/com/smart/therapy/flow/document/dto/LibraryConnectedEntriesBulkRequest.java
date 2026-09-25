package com.smart.therapy.flow.document.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LibraryConnectedEntriesBulkRequest {

    @NotEmpty(message = "entryIds must not be empty")
    private List<Long> entryIds;
}

