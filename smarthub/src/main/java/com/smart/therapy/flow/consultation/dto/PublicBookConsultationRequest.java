package com.smart.therapy.flow.consultation.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
public class PublicBookConsultationRequest {

    @NotNull(message = "Therapist ID is required")
    private Long therapistId;

    @NotNull(message = "Session start time is required")
    private Instant sessionStartUtc;

    @NotBlank(message = "Full name is required")
    private String clientFullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email")
    private String clientEmail;

    private String clientPhone;

    /** Optional; defaults to online / virtual when blank. */
    private String sessionMode;

    private String notes;

    private String clientTimezone;

    /**
     * Selected public counseling label (from public_site_services).
     * Hours still come from CONSULTATION; this only sets the session label.
     */
    private Long publicServiceId;

    /** Alternative to publicServiceId when id is unknown (e.g. slug=consultation). */
    private String publicServiceSlug;
}
