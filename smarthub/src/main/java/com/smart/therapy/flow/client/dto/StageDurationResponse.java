package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class StageDurationResponse {

    private String stage;
    private Long durationDays;
    private LocalDate startDate;
    private LocalDate endDate;
}

