package com.smart.therapy.flow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Slim client list DTO used by GET /clients (paginated list).
 *
 * Intentionally excludes insurance, emergency contact, address, and notes/demographics
 * fields to avoid the per-row normalized-entity lookups that {@link ClientResponse}
 * requires. Use {@link ClientResponse} (via GET /clients/{id}) when full details are needed.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSummaryResponse {

    private Long id;
    private String clientId;
    private String fullName;

    private String status;
    private String stage;

    private Long assignedTherapistId;
    private String assignedTherapistName;

    // Used by the clients list table (Ref: <referenceNumber>)
    private String referenceNumber;

    private Long checklistCount;
    private Long documentCount;

    private Instant lastSessionDate;
    private Instant nextAppointmentDate;
}
