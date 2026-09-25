package com.smart.therapy.flow.session.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.time.LocalDate;

@Data
@Schema(description = "Request to create recurring weekly sessions")
public class CreateRecurringSessionsRequest {

    @Valid
    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private CreateSessionRequest session;

    @Min(1)
    @Schema(description = "Number of weekly occurrences (optional if endDate provided)", example = "8")
    private Integer occurrences;

    @Schema(description = "End date in session timezone/local context (optional if occurrences provided)", example = "2026-12-31")
    private LocalDate endDate;
}
