package com.smart.therapy.flow.common.exception;

/**
 * Thrown when a tenant is not available for login or access (LOCKED, ARCHIVED, DELETED, or force-disabled).
 * Handled by GlobalExceptionHandler with 503 and code TENANT_MAINTENANCE or TENANT_FORCE_DISABLED.
 */
public class TenantUnavailableException extends RuntimeException {

    private final String code;

    public TenantUnavailableException(String message) {
        this(message, "TENANT_MAINTENANCE");
    }

    public TenantUnavailableException(String message, String code) {
        super(message);
        this.code = code != null ? code : "TENANT_MAINTENANCE";
    }

    public String getCode() {
        return code;
    }
}
