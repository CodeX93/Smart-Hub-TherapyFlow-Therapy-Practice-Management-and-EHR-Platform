package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to update an existing note. All fields are optional - only include fields you want to update.")
public class UpdateNoteRequest {
    @Schema(description = "Note title (optional)", example = "Phone call with client")
    private String title;

    @Schema(description = "Note content (optional)", example = "Client called to reschedule appointment")
    private String content;

    @Schema(description = "Type of note (optional)", example = "call", allowableValues = {"call", "email", "note", "general", "clinical", "supervisor"})
    private String noteType;

    @Schema(description = "Date of the event (optional, ISO 8601 format)", example = "2025-12-25T14:00:00Z", type = "string", format = "date-time")
    private Instant eventDate;

    @Schema(description = "Whether the note is private (optional)", example = "false")
    private Boolean isPrivate;
}

