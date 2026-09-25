package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class ConflictInfo {

    private Long sessionId;
    private Instant sessionDate;
    private Integer duration;
    private String clientName;
    private String therapistName;
    private String roomName;
}

