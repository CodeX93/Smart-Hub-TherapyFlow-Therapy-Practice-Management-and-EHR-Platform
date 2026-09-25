package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Therapist availability response for public API")
public class TherapistAvailabilityPublicResponse {

    @Schema(description = "Therapist ID", example = "1")
    private Long therapistId;

    @Schema(description = "Therapist name", example = "Dr. John Doe")
    private String therapistName;

    @Schema(description = "Date for which availability is checked", example = "2025-01-25")
    private LocalDate date;

    @Schema(description = "List of available time slots")
    private List<AvailableSlotResponse> availableSlots;

    @Schema(description = "Whether the therapist has any availability on this date", example = "true")
    private Boolean hasAvailability;
}
