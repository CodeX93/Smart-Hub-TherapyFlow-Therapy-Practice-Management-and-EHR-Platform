package com.smart.therapy.flow.session.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Slim session list/calendar DTO. Excludes clinical notes.
 * Zoom join URL is included so staff can open virtual meetings from scheduling.
 * Use {@link SessionResponse} for single-session detail (GET by id).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionSummaryResponse {

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
    private Boolean zoomEnabled;
    private String zoomMeetingId;
    private String zoomJoinUrl;
    private String zoomPassword;
    private String recurrenceGroupId;
    private Instant createdAt;
    private Instant updatedAt;
    private Long billingId;
    private Boolean hasInvoice;
    private Boolean hasTranscript;
}
