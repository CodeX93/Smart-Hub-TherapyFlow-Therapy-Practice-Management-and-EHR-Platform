package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotResponse {
    // Map of date (yyyy-MM-dd) to list of available time slots
    private Map<String, List<TimeSlot>> slotsByDate;

    /**
     * IANA zone the client's slots should be displayed in: their own portal setting,
     * or the clinic default when they have not chosen one. Clients read times in this
     * zone, never in whatever zone their device happens to be set to.
     */
    private String timezone;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimeSlot {
        private String start; // HH:mm format
        private String end;   // HH:mm format
        private String startUtc; // ISO-8601 UTC timestamp
        private String endUtc;   // ISO-8601 UTC timestamp
    }
}

