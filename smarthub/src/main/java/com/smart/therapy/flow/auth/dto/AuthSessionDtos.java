package com.smart.therapy.flow.auth.dto;

import java.time.Instant;
import java.util.List;

public final class AuthSessionDtos {
    private AuthSessionDtos() {}

    public record SessionItem(
            Long id,
            String deviceLabel,
            String ipAddress,
            String userAgent,
            Instant issuedAt,
            Instant lastActivityAt,
            Instant expiresAt,
            boolean current
    ) {}

    public record SessionListResponse(
            int activeCount,
            List<SessionItem> sessions
    ) {}
}
