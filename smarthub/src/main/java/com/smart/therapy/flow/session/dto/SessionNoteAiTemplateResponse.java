package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class SessionNoteAiTemplateResponse {
    Long id;
    String name;
    String instructions;
    Instant lastUsedAt;
    Instant createdAt;
    Instant updatedAt;
}
