package com.smart.therapy.flow.common.exception;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorResponse {
    private Instant timestamp;
    private Integer status;
    private String error;
    private String message;
    private String code;  // Error code for client-side handling
    private String path;  // Request path
    private String traceId;  // Correlation ID for distributed tracing
    private Map<String, Object> details;  // Additional error details
}

