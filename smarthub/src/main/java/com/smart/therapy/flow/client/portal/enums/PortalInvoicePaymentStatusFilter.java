package com.smart.therapy.flow.client.portal.enums;

import com.smart.therapy.flow.common.exception.BadRequestException;

import java.util.Arrays;
import java.util.Locale;

/**
 * Portal invoice list filter for {@code paymentStatus} query param.
 */
public enum PortalInvoicePaymentStatusFilter {
    UNPAID("unpaid"),
    PAID("paid"),
    PARTIAL("partial"),
    DENIED("denied"),
    CANCELLED("cancelled");

    private final String value;

    PortalInvoicePaymentStatusFilter(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PortalInvoicePaymentStatusFilter from(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new BadRequestException("Invalid payment status filter.");
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(values())
                .filter(v -> v.value.equals(normalized) || v.name().equalsIgnoreCase(normalized))
                .findFirst()
                .orElseThrow(() -> new BadRequestException(
                        "Invalid payment status filter. Allowed: unpaid, paid, partial, denied, cancelled"));
    }
}
