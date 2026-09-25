package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OnlineBookingRequestResponse {

    /** True when the therapist connected Zoom in the meantime, so there was nothing to ask for. */
    private Boolean onlineBookingAvailable;

    /** When the therapist was notified. Unchanged on a repeat request inside the cooldown. */
    private Instant requestedAt;

    /** When the client may ask again. */
    private Instant nextRequestAllowedAt;
}
