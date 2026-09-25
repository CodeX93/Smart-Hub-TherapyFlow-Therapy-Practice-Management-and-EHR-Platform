package com.smart.therapy.flow.document.dto;

import com.smart.therapy.flow.document.enums.ConnectionType;
import lombok.Builder;
import lombok.Value;

import java.time.Instant;
import java.util.List;

@Value
@Builder
public class LibraryConnectedEntryResponse {
    Long connectionId;
    Long fromEntryId;
    Long toEntryId;
    Long entryId;
    String entryTitle;
    String entryContent;
    List<String> tags;
    Long categoryId;
    String categoryName;
    ConnectionType connectionType;
    Integer connectionStrength;
    String description;
    Boolean active;
    Instant createdAt;
    Instant updatedAt;
}

