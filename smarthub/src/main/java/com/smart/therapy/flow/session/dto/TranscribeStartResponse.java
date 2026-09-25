package com.smart.therapy.flow.session.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TranscribeStartResponse {
    private String uploadId;
    private String websocketTicket;
    private long websocketTicketExpiresInSeconds;
}
