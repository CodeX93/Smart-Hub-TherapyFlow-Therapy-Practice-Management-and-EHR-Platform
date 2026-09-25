package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LibraryBulkImportResponse {
    int total;
    int successful;
    int skipped;
    int failed;
    int categoriesCreated;
    int connectionsCreated;
    List<ImportError> errors;

    @Value
    @Builder
    public static class ImportError {
        int row;
        String title;
        String error;
    }
}

