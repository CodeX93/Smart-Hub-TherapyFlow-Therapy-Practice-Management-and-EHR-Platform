package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalAppointmentResponse {
    private Long id;
    private String sessionDate; // yyyy-MM-dd
    private String sessionTime; // HH:mm
    private Integer duration;
    private String sessionType;
    private String sessionMode;
    private String status;
    private String location;
    private String roomName;
    private String referenceNumber;
    private String serviceCode;
    private String serviceName;
    private BigDecimal serviceRate;
    private String therapistName;
    /** Client-submitted session rating (0–10), if already rated. */
    private Integer clientRating;
    /** Optional comment submitted with the client rating. */
    private String clientRatingComment;
}

