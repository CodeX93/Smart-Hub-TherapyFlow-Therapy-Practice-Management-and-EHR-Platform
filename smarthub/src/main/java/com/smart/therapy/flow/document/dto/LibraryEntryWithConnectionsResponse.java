package com.smart.therapy.flow.document.dto;

import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class LibraryEntryWithConnectionsResponse {
    LibraryEntryResponse entry;
    List<LibraryConnectedEntryResponse> connectedEntries;
}

