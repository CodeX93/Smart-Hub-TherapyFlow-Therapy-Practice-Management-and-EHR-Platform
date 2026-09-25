package com.smart.therapy.flow.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AvailableSlotResponse {
    private Instant time; // UTC time
    private String timezone; // Timezone used for calculation (therapist's timezone)
    private LocalDateTime localTime; // Local time in therapist's timezone
    private boolean available;
    private boolean therapistBusy;
    private boolean roomBusy;

    /**
     * Booking modality for this slot derived from the therapist shift:
     * {@code online}, {@code in-person}. Present for public consultation flows so the
     * visitor does not choose modality — admin/therapist schedule decides.
     */
    private String sessionMode;
    
    // Optional: Client's local time if client timezone is provided
    private LocalDateTime clientLocalTime;
    private String clientTimezone;
}

