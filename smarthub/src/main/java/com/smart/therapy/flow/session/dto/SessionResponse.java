package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionResponse {

    private Long id;
    private Long clientId;
    private String clientName;
    private Long therapistId;
    private String therapistName;
    private Instant sessionDate;
    private Integer duration;
    private String sessionType;
    private String sessionMode;
    private String status;
    private Long serviceId;
    private String serviceName;
    private Long roomId;
    private String roomName;
    private String notes;
    private Boolean zoomEnabled;
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomPassword;
    private String recurrenceGroupId;
    private Instant createdAt;
    private Instant updatedAt;
}

