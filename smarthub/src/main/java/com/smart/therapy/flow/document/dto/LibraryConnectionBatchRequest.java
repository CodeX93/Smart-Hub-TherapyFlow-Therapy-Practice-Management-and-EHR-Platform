package com.smart.therapy.flow.document.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class LibraryConnectionBatchRequest {

    @NotEmpty(message = "Connections must not be empty")
    private List<@Valid LibraryConnectionRequest> connections;
}

