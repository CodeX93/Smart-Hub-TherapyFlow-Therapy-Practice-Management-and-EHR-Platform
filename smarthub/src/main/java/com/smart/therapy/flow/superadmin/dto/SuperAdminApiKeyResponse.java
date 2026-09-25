package com.smart.therapy.flow.superadmin.dto;

import java.time.Instant;
import java.util.List;

public record SuperAdminApiKeyResponse(
        Long id,
        String keyName,
        String keyPrefix,
        List<String> scopes,
        Boolean active,
        Instant expiresAt,
        Instant createdAt,
        Instant lastUsedAt,
        Instant revokedAt
) {}
