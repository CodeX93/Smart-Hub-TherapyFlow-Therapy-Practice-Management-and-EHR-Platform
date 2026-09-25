package com.smart.therapy.flow.consultation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicBookConsultationResponse {
    private Long sessionId;
    private Long therapistId;
    private String therapistName;
    private Instant sessionStartUtc;
    private Integer durationMinutes;
    private String serviceCode;
    private String status;
    private String message;
    /** Zoom join URL when an online meeting was created for the booking. */
    private String joinUrl;
    /** Public counseling label chosen by the visitor. */
    private Long publicServiceId;
    private String publicServiceName;
    private BigDecimal baseRate;
}
