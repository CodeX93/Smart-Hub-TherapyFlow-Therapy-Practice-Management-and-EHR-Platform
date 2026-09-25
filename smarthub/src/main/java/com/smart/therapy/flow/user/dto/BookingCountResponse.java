package com.smart.therapy.flow.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Booking count response for a therapist")
public class BookingCountResponse {

    @Schema(description = "Therapist ID", example = "1")
    private Long therapistId;

    @Schema(description = "Therapist name", example = "Dr. John Doe")
    private String therapistName;

    @Schema(description = "Total number of bookings (all time)", example = "150")
    private Long totalBookings;

    @Schema(description = "Number of upcoming bookings", example = "25")
    private Long upcomingBookings;

    @Schema(description = "Number of completed bookings", example = "120")
    private Long completedBookings;

    @Schema(description = "Number of cancelled bookings", example = "5")
    private Long cancelledBookings;

    @Schema(description = "Date range start (optional filter)", example = "2025-01-01T00:00:00Z")
    private Instant startDate;

    @Schema(description = "Date range end (optional filter)", example = "2025-12-31T23:59:59Z")
    private Instant endDate;
}
