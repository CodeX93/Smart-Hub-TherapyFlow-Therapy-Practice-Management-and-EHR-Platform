package com.smart.therapy.flow.auth.dto;

import lombok.Data;

/**
 * Request DTO for logout endpoint.
 * Optionally includes refresh token to blacklist it as well.
 */
@Data
public class LogoutRequest {
    
    /**
     * Optional refresh token to blacklist.
     * If provided, both access and refresh tokens will be blacklisted.
     */
    private String refreshToken;
}

