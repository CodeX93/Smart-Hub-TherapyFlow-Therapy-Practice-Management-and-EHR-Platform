package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Zoom meeting details for starting a meeting")
public class ZoomMeetingDetailsResponse {

    @Schema(description = "Session ID", example = "1")
    private Long sessionId;

    @Schema(description = "Zoom meeting ID", example = "123456789")
    private String meetingId;

    @Schema(description = "Zoom join URL", example = "https://zoom.us/j/123456789")
    private String joinUrl;

    @Schema(description = "Zoom meeting password (if required)", example = "123456")
    private String password;
}
