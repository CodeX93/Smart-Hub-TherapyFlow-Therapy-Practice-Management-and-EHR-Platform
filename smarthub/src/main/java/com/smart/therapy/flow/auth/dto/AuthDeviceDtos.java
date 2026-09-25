package com.smart.therapy.flow.auth.dto;

import java.time.Instant;
import java.util.List;

public final class AuthDeviceDtos {
    private AuthDeviceDtos() {}

    public record DeviceItem(
            Long id,
            String deviceLabel,
            String ipAddress,
            String userAgent,
            Instant firstSeenAt,
            Instant lastSeenAt,
            boolean trusted,
            Instant trustExpiresAt,
            boolean current
    ) {}

    public record DeviceListResponse(
            int count,
            int trustedCount,
            List<DeviceItem> devices
    ) {}
}
