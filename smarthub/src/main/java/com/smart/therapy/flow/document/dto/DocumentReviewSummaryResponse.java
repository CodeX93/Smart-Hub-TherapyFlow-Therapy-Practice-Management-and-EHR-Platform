package com.smart.therapy.flow.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentReviewSummaryResponse {
    private long totalPending;
    private long overdue;
    private long therapistReview;
    private long supervisorReview;
    private long pending;
}
