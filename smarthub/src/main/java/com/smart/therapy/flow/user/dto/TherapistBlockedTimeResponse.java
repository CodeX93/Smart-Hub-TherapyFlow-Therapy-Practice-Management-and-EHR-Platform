package com.smart.therapy.flow.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class TherapistBlockedTimeResponse {
    private Long id;
    private Long therapistId;
    private String therapistName;
    private Instant startTime;
    private Instant endTime;
    private Boolean allDay;
    private BlockType blockType;
    private String reason;
    private Boolean isRecurring;
    private String recurrencePattern; // JSON string
    private Boolean isActive;
    private Instant createdAt;
    private Instant updatedAt;
}

