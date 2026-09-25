package com.smart.therapy.flow.notification.service;

import org.springframework.util.StringUtils;

/**
 * Safe log formatting for Twilio SMS (masks phone numbers and secrets).
 */
final class TwilioSmsLogUtil {

    private TwilioSmsLogUtil() {
    }

    static String maskPhone(String phone) {
        if (!StringUtils.hasText(phone)) {
            return "(empty)";
        }
        String trimmed = phone.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, Math.min(4, trimmed.length())) + "****" + trimmed.substring(trimmed.length() - 2);
    }

    static String maskAccountSid(String accountSid) {
        if (!StringUtils.hasText(accountSid)) {
            return "(empty)";
        }
        String trimmed = accountSid.trim();
        if (trimmed.length() <= 10) {
            return "***";
        }
        return trimmed.substring(0, 6) + "..." + trimmed.substring(trimmed.length() - 4);
    }
}
