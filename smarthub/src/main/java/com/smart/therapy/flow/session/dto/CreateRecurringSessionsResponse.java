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
public class CreateRecurringSessionsResponse {
    private String groupId;
    private List<SessionResponse> created;
    private int createdCount;
    private List<SkippedRecurringOccurrence> skipped;
    private int skippedCount;
    private String warning;

    /** @deprecated Legacy fields retained for backward compatibility */
    @Deprecated
    private Integer requested;
    /** @deprecated */
    @Deprecated
    private Integer failed;
    /** @deprecated */
    @Deprecated
    private List<SessionResponse> sessions;
    /** @deprecated */
    @Deprecated
    private List<String> errors;
}
