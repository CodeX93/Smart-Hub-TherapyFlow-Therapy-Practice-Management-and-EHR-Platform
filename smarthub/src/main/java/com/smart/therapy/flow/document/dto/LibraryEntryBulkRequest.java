package com.smart.therapy.flow.document.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class LibraryEntryBulkRequest {

    /** Active library tab — every imported entry is filed under this category. */
    @NotNull(message = "Category id is required")
    private Long categoryId;

    @NotEmpty(message = "Entries list cannot be empty")
    private List<@Valid LibraryEntryBulkItem> entries;

    @Data
    public static class LibraryEntryBulkItem {
        private String domain;
        private String subdomain;

        @NotNull(message = "Title is required")
        private String title;

        @NotNull(message = "Content is required")
        private String content;

        private List<String> tags;
        private Integer sortOrder;
    }
}

