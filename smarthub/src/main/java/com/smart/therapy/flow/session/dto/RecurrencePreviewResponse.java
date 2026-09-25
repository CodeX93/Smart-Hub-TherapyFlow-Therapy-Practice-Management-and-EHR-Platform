package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrencePreviewResponse {
    private List<RecurrencePreviewOccurrence> sessions;
    private int totalRequested;
    private int freeCount;
    private int conflictCount;
}
