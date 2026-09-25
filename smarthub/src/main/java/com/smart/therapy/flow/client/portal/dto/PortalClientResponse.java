package com.smart.therapy.flow.client.portal.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortalClientResponse {
    private Long id;
    private String clientId;
    private String fullName;
    private String email;
    private String phone;
    private Long assignedTherapistId;
    private String timezone; // IANA timezone ID (e.g., "America/New_York", "Asia/Karachi")
    private String avatarUrl; // Optional profile avatar URL (after upload)
    /** False when the assigned therapist has no active Zoom integration, so the portal can disable the online option. */
    private Boolean onlineBookingAvailable;
    /** Set when the client already asked the therapist to enable online sessions; drives the tooltip's sent state. */
    private java.time.Instant onlineBookingRequestedAt;
}

