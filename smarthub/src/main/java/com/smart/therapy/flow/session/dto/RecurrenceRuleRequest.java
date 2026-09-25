package com.smart.therapy.flow.session.dto;

import com.smart.therapy.flow.session.enums.RecurrenceEndMode;
import com.smart.therapy.flow.session.enums.RecurrenceType;
import com.smart.therapy.flow.session.validation.ValidRecurrenceRule;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

@Data
@ValidRecurrenceRule
@Schema(description = "Recurrence rule for weekly or monthly session series")
public class RecurrenceRuleRequest {

    @NotNull(message = "Client ID is required")
    private Long clientId;

    @NotNull(message = "Therapist ID is required")
    private Long therapistId;

    @NotNull(message = "Service ID is required")
    private Long serviceId;

    private Long roomId;

    @NotNull(message = "Session mode is required")
    @Schema(description = "Session mode option key (e.g. in_person, Online, phone)")
    private String sessionMode;

    @Schema(description = "Clinical session type option key (e.g. assessment, individual)")
    private String sessionType;

    private String notes;

    @Schema(defaultValue = "false")
    private Boolean zoomEnabled = false;

    @NotNull(message = "Session date is required")
    @FutureOrPresent(message = "Session date cannot be in the past")
    @Schema(description = "Date and time of the first session (ISO 8601 format)", example = "2026-06-16T06:00:00.000Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private Instant sessionDate;

    @Schema(description = "IANA timezone ID for the session (e.g., 'Asia/Karachi'). If not provided, uses therapist's timezone.", example = "Asia/Karachi")
    private String timezone;

    @NotNull(message = "Recurrence type is required")
    private RecurrenceType recurrenceType;

    @Schema(description = "Days of week (0=Sunday … 6=Saturday). Required for weekly recurrence.")
    private List<@Min(0) @Max(6) Integer> daysOfWeek;

    @Schema(description = "Months of year (1=January … 12=December). Optional filter for monthly recurrence.")
    private List<@Min(1) @Max(12) Integer> monthsOfYear;

    @Min(value = 1, message = "Interval must be at least 1")
    @Max(value = 8, message = "Interval cannot exceed 8")
    @Schema(defaultValue = "1")
    private Integer interval = 1;

    @NotNull(message = "End mode is required")
    private RecurrenceEndMode endMode;

    @Min(value = 1, message = "Count must be at least 1")
    @Max(value = 60, message = "Count cannot exceed 60")
    private Integer count;

    @Schema(description = "End date in session timezone (optional if count provided)")
    private LocalDate untilDate;
}
