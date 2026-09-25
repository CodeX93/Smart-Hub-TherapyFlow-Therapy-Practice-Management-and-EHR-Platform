package com.smart.therapy.flow.notification.enums;

/**
 * Normalized Twilio SMS error categories for auditing and callers.
 */
public enum TwilioSmsErrorCode {
    NOT_CONFIGURED,
    INVALID_CONFIGURATION,
    AUTHENTICATION_FAILED,
    INSUFFICIENT_CREDITS,
    ACCOUNT_SUSPENDED,
    INVALID_DESTINATION,
    DESTINATION_OPTED_OUT,
    GEO_PERMISSION_DENIED,
    PROVIDER_ERROR,
    UNKNOWN
}
