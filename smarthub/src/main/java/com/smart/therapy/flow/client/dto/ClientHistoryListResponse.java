package com.smart.therapy.flow.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientHistoryListResponse {
    private List<ClientHistoryResponse> history;
    private String message; // Message shown when history is empty
    private int count; // Number of history entries
}

