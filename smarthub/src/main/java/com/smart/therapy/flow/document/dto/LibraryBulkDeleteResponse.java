package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LibraryBulkDeleteResponse {
    int total;
    int deleted;
    int failed;
    List<DeleteError> errors;

    @Value
    @Builder
    public static class DeleteError {
        Long entryId;
        String error;
    }
}

