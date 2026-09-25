package com.smart.therapy.flow.integration.zoom.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ZoomMeetingResponse {
    private Long meetingId;
    private String hostId;
    private String topic;
    private String startTime;
    private Integer duration;
    private String timezone;
    private String joinUrl;
    private String password;
}

