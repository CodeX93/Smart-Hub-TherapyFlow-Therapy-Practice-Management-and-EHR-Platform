package com.smart.therapy.flow.client.dto;

import com.smart.therapy.flow.session.dto.SessionResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClientSessionConflictsResponse {
    private Long clientId;
    private List<ConflictGroup> conflicts;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConflictGroup {
        private LocalDate date;
        private List<SessionResponse> sessions;
    }
}

