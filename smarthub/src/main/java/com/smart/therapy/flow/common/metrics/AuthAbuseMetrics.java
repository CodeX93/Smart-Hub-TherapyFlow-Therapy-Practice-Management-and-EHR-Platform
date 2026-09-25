package com.smart.therapy.flow.common.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Local-visible Micrometer counters for auth-abuse / access-denial signals.
 * Azure Monitor alert rules are documented in {@code docs/AZURE_MONITOR_AUTH_ABUSE_ALERTS.md}.
 */
@Component
@RequiredArgsConstructor
public class AuthAbuseMetrics {

    public static final String FAILED_LOGIN = "therapyflow.auth.failed_login";
    public static final String MFA_LOCKOUT = "therapyflow.auth.mfa_lockout";
    public static final String HTTP_403 = "therapyflow.auth.http_403";
    public static final String HTTP_429 = "therapyflow.auth.http_429";

    private final ObjectProvider<MeterRegistry> meterRegistryProvider;

    public void incrementFailedLogin() {
        increment(FAILED_LOGIN);
    }

    public void incrementMfaLockout() {
        increment(MFA_LOCKOUT);
    }

    public void incrementForbidden() {
        increment(HTTP_403);
    }

    public void incrementRateLimited() {
        increment(HTTP_429);
    }

    private void increment(String name) {
        MeterRegistry registry = meterRegistryProvider != null ? meterRegistryProvider.getIfAvailable() : null;
        if (registry != null) {
            registry.counter(name).increment();
        }
    }
}
