package com.smart.therapy.flow.document.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.Instant;

@Data
@Schema(description = "Request to create a client note")
public class CreateNoteRequest {
    @NotNull
    @Schema(description = "ID of the client (REQUIRED)", example = "123", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long clientId;
    
    @Schema(description = "Note title (optional)", example = "Phone call with client")
    private String title;
    
    @NotNull
    @Schema(description = "Note content (REQUIRED)", example = "Client called to reschedule appointment. Expressed concern about upcoming session.", requiredMode = Schema.RequiredMode.REQUIRED)
    private String content;
    
    @Schema(description = "Type of note (optional)", example = "call", allowableValues = {"call", "email", "note", "general", "clinical", "supervisor"})
    private String noteType; // call, email, note, general, clinical, supervisor
    
    @NotNull
    @Schema(description = "Date of the event (REQUIRED, ISO 8601 format)", example = "2025-12-25T14:00:00Z", requiredMode = Schema.RequiredMode.REQUIRED, type = "string", format = "date-time")
    private Instant eventDate;
    
    @Schema(description = "Whether the note is private (optional, default: false)", example = "false", defaultValue = "false")
    private Boolean isPrivate;
}

