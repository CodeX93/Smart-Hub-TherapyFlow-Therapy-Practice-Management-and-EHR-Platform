package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.ConnectionType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class LibraryConnectionResponse {
    Long id;
    Long fromEntryId;
    String fromEntryTitle;
    Long toEntryId;
    String toEntryTitle;
    ConnectionType connectionType;
    Integer strength;
    String description;
    Long createdByUserId;
    String createdByName;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}

