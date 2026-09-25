package com.smart.therapy.flow.session.dto;

import com.smart.therapy.flow.session.enums.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Minimal session payload for scheduling calendar views (month/week/day).
 * Avoids loading Zoom secrets, audit timestamps, and full PHI client rows.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SessionCalendarItemResponse {

    private Long id;
    private Long clientId;
    private String clientName;
    private Long therapistId;
    private String therapistName;
    private Instant sessionDate;
    /** Practice-local calendar day (ISO-8601 date) for month/week cell bucketing. */
    private String calendarDate;
    private Integer duration;
    private String sessionType;
    private String sessionMode;
    private String status;
    private String serviceName;
    private Long roomId;
    private String roomName;
    private Boolean zoomEnabled;
    private String zoomJoinUrl;
    private String recurrenceGroupId;
    private Long billingId;
    private Boolean hasTranscript;

    /**
     * JPQL constructor projection (roomType used only to derive online mode).
     */
    public SessionCalendarItemResponse(
            Long id,
            Long clientId,
            String clientName,
            Long therapistId,
            String therapistName,
            Instant sessionDate,
            Integer duration,
            String clinicalSessionType,
            String sessionMode,
            String status,
            String serviceName,
            Long roomId,
            String roomName,
            RoomType roomType,
            String recurrenceGroupId,
            Long billingId) {
        this.id = id;
        this.clientId = clientId;
        this.clientName = clientName;
        this.therapistId = therapistId;
        this.therapistName = therapistName;
        this.sessionDate = sessionDate;
        this.duration = duration;
        this.sessionType = clinicalSessionType;
        this.sessionMode = sessionMode;
        this.status = status;
        this.serviceName = serviceName;
        this.roomId = roomId;
        this.roomName = roomName;
        this.recurrenceGroupId = recurrenceGroupId;
        this.billingId = billingId;
        this.hasTranscript = false;
        this.zoomEnabled = false;
        if (roomType == RoomType.VIRTUAL
                && (sessionMode == null || sessionMode.isBlank()
                || (!sessionMode.equalsIgnoreCase("online")
                && !sessionMode.equalsIgnoreCase("virtual")
                && !sessionMode.equalsIgnoreCase("telehealth")
                && !sessionMode.equalsIgnoreCase("video")))) {
            this.sessionMode = "online";
        }
    }
}
