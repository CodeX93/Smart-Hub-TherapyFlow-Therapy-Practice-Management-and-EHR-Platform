package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
public class AvailabilityResponse {

    private LocalDate date;
    private Long therapistId;
    private Long roomId;
    private List<TimeSlot> timeSlots;
}

