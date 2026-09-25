package com.smart.therapy.flow.user.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class CreateTherapistBlockedTimeRequest {
    @NotNull(message = "Therapist ID is required")
    private Long therapistId;
    
    @NotNull(message = "Start time is required")
    private Instant startTime;
    
    @NotNull(message = "End time is required")
    private Instant endTime;
    
    private Boolean allDay;
    
    @NotNull(message = "Block type is required")
    private BlockType blockType;
    
    private String reason;
    
    private Boolean isRecurring;
    
    private String recurrencePattern; // JSON string
    
    private Boolean isActive;
}

