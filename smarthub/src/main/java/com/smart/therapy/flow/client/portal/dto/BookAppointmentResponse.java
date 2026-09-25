package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookAppointmentResponse {
    private String message;
    private AppointmentInfo appointment;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AppointmentInfo {
        private Long id;
        private String sessionDate; // yyyy-MM-dd
        private String sessionTime; // HH:mm
        private Integer duration;
        private String sessionType;
        private String sessionMode;
        private String status;
        private String location;
        private Boolean zoomEnabled;
        private String zoomJoinUrl;
        private String zoomPassword;
    }
}

