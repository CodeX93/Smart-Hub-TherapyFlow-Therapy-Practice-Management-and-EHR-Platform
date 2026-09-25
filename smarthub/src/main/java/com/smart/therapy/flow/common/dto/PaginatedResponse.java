package com.smart.therapy.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Generic paginated response wrapper to provide consistent pagination metadata
 * across the API surface. Controllers should return this instead of exposing
 * Spring Data's {@code Page} directly to avoid leaking internal details.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {

    private List<T> items;
    private long totalCount;
    private int page;
    private int pageSize;
    private int totalPages;

    public static <T> PaginatedResponse<T> of(List<T> data, long total, int page, int pageSize) {
        int calculatedTotalPages = pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
        return PaginatedResponse.<T>builder()
                .items(data)
                .totalCount(total)
                .page(page)
                .pageSize(pageSize)
                .totalPages(calculatedTotalPages)
                .build();
    }
}


