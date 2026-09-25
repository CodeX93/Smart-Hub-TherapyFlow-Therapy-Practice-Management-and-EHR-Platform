package com.smart.therapy.flow.client.dto;

import lombok.Builder;
import lombok.Value;

import java.time.Instant;

@Value
@Builder
public class ClientSmsLogResponse {
    Long id;
    String action;
    String result;
    String resourceId;
    Instant timestamp;
    String details;
}
