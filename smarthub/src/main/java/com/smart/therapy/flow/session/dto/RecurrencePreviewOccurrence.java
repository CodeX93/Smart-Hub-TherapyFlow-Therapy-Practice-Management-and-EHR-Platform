package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecurrencePreviewOccurrence {
    private Instant sessionDate;
    private String localDate;
    private String sessionTime;
    private boolean hasConflict;
    private List<String> reasons;
}
