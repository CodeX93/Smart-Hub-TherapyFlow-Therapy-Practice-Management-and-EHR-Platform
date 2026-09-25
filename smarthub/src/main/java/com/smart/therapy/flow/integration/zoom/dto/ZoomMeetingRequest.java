package com.smart.therapy.flow.integration.zoom.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ZoomMeetingRequest {
    private String clientName;
    private String therapistName;
    private Instant sessionDate;
    private Integer duration; // minutes
    private String timezone; // IANA timezone ID (e.g., "America/New_York", "America/Los_Angeles")
}

