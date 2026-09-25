package com.smart.therapy.flow.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Standard success response DTO for operations that don't return data.
 * Use this instead of ResponseEntity<Void> for better API consistency.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SuccessResponse {
    private Boolean success;
    private String message;
    private Instant timestamp;
    
    public static SuccessResponse of(String message) {
        return SuccessResponse.builder()
                .success(true)
                .message(message)
                .timestamp(Instant.now())
                .build();
    }
    
    public static SuccessResponse ok() {
        return SuccessResponse.builder()
                .success(true)
                .message("Operation completed successfully")
                .timestamp(Instant.now())
                .build();
    }
}

