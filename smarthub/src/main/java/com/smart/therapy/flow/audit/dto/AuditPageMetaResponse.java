package com.smart.therapy.flow.audit.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditPageMetaResponse {
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;
}
