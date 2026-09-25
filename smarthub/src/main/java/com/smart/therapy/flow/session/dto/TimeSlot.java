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
public class TimeSlot {

    private Instant time;
    private Instant datetime;
    private Boolean available;
    private Boolean therapistBusy;
    private Boolean roomBusy;
}

